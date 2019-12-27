package softrpc.framework.invoker;

import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import softrpc.framework.loadBalance.common.LoadBalanceEngine;
import softrpc.framework.serialization.message.RequestMessage;
import softrpc.framework.serialization.message.ResponseMessage;
import softrpc.framework.utils.PropertyConfigUtil;
import softrpc.framework.zookeeper.RegisterCenter;
import softrpc.framework.zookeeper.RegisterCenter4Invoker;
import softrpc.framework.zookeeper.message.ProviderRegisterMessage;

import java.lang.reflect.*;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * 客户端代理工厂类，为每个rpc服务接口的引用对象都由getProxyInstance产生
 *
 * @author xctian
 * @date 2019/12/25
 */
public class ClientProxyBeanFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientProxyBeanFactory.class);

    /**
     * 饿汉单例
     */
    private static ClientProxyBeanFactory instance = new ClientProxyBeanFactory();
    /**
     * 客户端注册中心
     */
    private static RegisterCenter4Invoker registerCenter4Invoker = RegisterCenter.getInstance();
    /**
     * 线程池：RPC服务调用都需要走此线程池中的线程
     */
    private static ExecutorService threadPool;
    /**
     * soft-reference/soft-service配置文件中标签的属性默认值的等价字符串
     */
    private static final String DEFAULT_VALUE_IN_LABEL = "default";

    private ClientProxyBeanFactory() {
    }

    public static ClientProxyBeanFactory getInstance() {
        return instance;
    }

    /**
     * 通过动态代理，生成引用服务的代理对象
     *
     * @param appName             应用名
     * @param serviceInterface    接口名
     * @param consumeTimeout      超时时间
     * @param loadBalanceStrategy 负载均衡策略
     * @param <T>                 代理对象的实际类型
     * @return 生成的代理对象
     */
    public static <T> T getProxyInstance(final String appName, final Class<T> serviceInterface, final int consumeTimeout, final String loadBalanceStrategy) {
        long startTime = System.currentTimeMillis();
        // 动态代理
        Object proxy = Proxy.newProxyInstance(serviceInterface.getClassLoader(), new Class[]{serviceInterface}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // 获取该接口的服务地址
                String nameSpace = appName + "/" + serviceInterface.getName();
                List<ProviderRegisterMessage> providerRegisterMessages = registerCenter4Invoker.getProviderMap().get(nameSpace);
                // 判断是否采用默认的负载均衡策略
                String strategy = loadBalanceStrategy;
                if (DEFAULT_VALUE_IN_LABEL.equalsIgnoreCase(strategy)) {
                    strategy = PropertyConfigUtil.getDefaultClusterStrategy();
                }
                // 根据负载均衡策略选取provider
                ProviderRegisterMessage providerRegisterMessage = LoadBalanceEngine.select(providerRegisterMessages, strategy);
                if (null == providerRegisterMessage) {
                    throw new RuntimeException("无可用服务节点");
                }
                // 封装请求消息的内容
                RequestMessage requestMessage = new RequestMessage();
                // 标识请求消息的ID,随机生成
                requestMessage.setMessageId(UUID.randomUUID().toString());
                // 服务方接口实现类的bean标签的id(经过负载均衡后)
                requestMessage.setRefId(providerRegisterMessage.getRefId());
                // 服务限流大小
                requestMessage.setMaxWorkThread(providerRegisterMessage.getWorkThread());
                // 服务调用超时时间
                requestMessage.setTimeout(consumeTimeout);
                // 服务接口名称(接口全限定名)
                requestMessage.setServicePath(serviceInterface.getName());
                // 调用方法名
                requestMessage.setMethodName(method.getName());
                // 设置方法和参数类型，用于反射调用
                if (null != args && args.length > 0) {
                    // 实际传参
                    requestMessage.setParameters(args);
                    // 初始化方法参数类型列表
                    requestMessage.setParameterTypes(new String[args.length]);
                    // java.lang.reflect.Method.getGenericParameterTypes()方法返回一个Type对象的数组，它以声明顺序表示此Method对象表
                    // 示的方法的形式参数类型(含泛型），并且是全限定名
                    Type[] types = method.getGenericParameterTypes();
                    // 参数化类型(ParameterType)举例：如List<String> 这就是个参数化类型，而 List 就是个原生类型。
                    for (int i = 0; i < args.length; i++) {
                        // 根据反射调用的特点，如果参数的类型是ParameterType，则需要找到它的原生类型
                        if (types[i] instanceof ParameterizedType) {
                            requestMessage.getParameterTypes()[i] = ((ParameterizedType) types[i]).getRawType().getTypeName();
                        } else {
                            requestMessage.getParameterTypes()[i] = types[i].getTypeName();
                        }
                    }
                }
                Future<ResponseMessage> responseMessageFuture = null;
                try {
                    if (null == threadPool) {
                        synchronized (ClientProxyBeanFactory.class) {
                            if (null == threadPool) {
                                int corePoolSize = PropertyConfigUtil.getThreadWorkerSize();
                                threadPool = new ThreadPoolExecutor(corePoolSize, corePoolSize, 0, TimeUnit.MILLISECONDS,
                                        new ArrayBlockingQueue<>(100), new DefaultThreadFactory("Soft-Rpc-InvokerPool"), new ThreadPoolExecutor.DiscardPolicy());
                            }
                        }
                    }
                    String serverIp = providerRegisterMessage.getServerIp();
                    int serverPort = providerRegisterMessage.getServerPort();
                    InetSocketAddress inetSocketAddress = new InetSocketAddress(serverIp, serverPort);
                    // 任务扔到线程池执行
                    responseMessageFuture = threadPool.submit(new RevokerServiceCallable(inetSocketAddress, requestMessage));
                    //阻塞等待response
                    ResponseMessage response = responseMessageFuture.get(requestMessage.getTimeout(), TimeUnit.MILLISECONDS);
                    // 返回结果
                    return response.getResultValue();
                } catch (InterruptedException e) {
                    LOGGER.error("请求超时，线程中断！");
                    responseMessageFuture.cancel(true);
                }
                return null;
            }
        });
        long duration = System.currentTimeMillis() - startTime;
        LOGGER.info("创建代理对象耗时{}ms:[{}]", duration, serviceInterface.getName());
        return (T) proxy;
    }
}
