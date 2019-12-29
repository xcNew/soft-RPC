package softrpc.framework.spring.factoryBean;

import com.google.common.collect.Sets;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import softrpc.framework.invoker.ClientProxyBeanFactory;
import softrpc.framework.invoker.NettyChannelPoolFactory;
import softrpc.framework.zookeeper.RegisterCenter;
import softrpc.framework.zookeeper.message.InvokerRegisterMessage;
import softrpc.framework.zookeeper.message.ProviderRegisterMessage;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;

/**
 * RpcReferenceFactoryBean类
 * 由于parser类的作用，对于标签的解析生成的是一个已经组装了的factoryBean对象，而factoryBean返回的实际对象是getObject里指定的类对象
 *
 * @author xctian
 * @date 2019/12/18
 */
public class RpcReferenceFactoryBean implements FactoryBean, InitializingBean {

    /**
     * 缓存的服务地址集合（IP+PORT)
     */
    private static Set<InetSocketAddress> socketAddressSet = Sets.newHashSet();

    /**
     * ChanelPool 工厂
     */
    private static NettyChannelPoolFactory nettyChannelPoolFactory = NettyChannelPoolFactory.getInstance();

    /**
     * 注册中心
     */
    RegisterCenter registerCenter = RegisterCenter.getInstance();

    /* 标签中必须配置的参数 */
    /**
     * 服务接口
     */
    private Class<?> targetInterface;
    /**
     * 超时时间
     */
    private int timeout;
    /**
     * 服务所属应用名称
     */
    private String appName;

    /* soft-reference中可选配置项 */
    /**
     * 服务分组名（本项目没用到，可以自行在注册中心进行扩展）
     * 如果soft-reference没有配置，则不会执行parser里对应的if对该变量赋值，该变量则为默认值default，下同。
     */
    private String groupName = "default";
    /* 本地使用参数，不需要传到ZK
    /**
     * 负载均衡策略
     */
    private String loadBalanceStrategy = "default";


    /**
     * 生成soft:reference标签所引用的服务接口的代理对象
     *
     * @return 服务接口的代理对象
     */
    @Override
    public Object getObject() throws Exception {
        return ClientProxyBeanFactory.getProxyInstance(appName, targetInterface, timeout, loadBalanceStrategy);
    }

    /**
     * 声明接口代理对象的类型
     *
     * @return
     */
    @Override
    public Class<?> getObjectType() {
        return targetInterface;
    }

    /**
     * 声明是否单例
     *
     * @return
     */
    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
     * invoker初始化：从ZK获取引用服务的地址，并为每一个地址生成channelPool
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // 将soft-reference内容注册到ZK，同时获取服务地址到本地
        InvokerRegisterMessage invoker = new InvokerRegisterMessage();
        invoker.setServicePath(targetInterface.getName());
        invoker.setGroupName(groupName);
        invoker.setAppName(appName);
        // 本机所有invoker的machineID是相同的
        invoker.setInvokerMachineID4Server(InvokerRegisterMessage.getInvokerMachineID4Client());
        // 根据标签内容从注册中心获取服务地址列表
        List<ProviderRegisterMessage> providerRegisterMessageList = registerCenter.registerInvoker(invoker);
        // 提前为不同的服务地址创建channelPool
        for (ProviderRegisterMessage provider : providerRegisterMessageList) {
            InetSocketAddress socketAddress = new InetSocketAddress(provider.getServerIp(), provider.getServerPort());
            boolean isFirstAdd = socketAddressSet.add(socketAddress);
            if (isFirstAdd) {
                nettyChannelPoolFactory.registerChannelQueueToMap(socketAddress);
            }
        }
    }

    public static Set<InetSocketAddress> getSocketAddressSet() {
        return socketAddressSet;
    }

    public static void setSocketAddressSet(Set<InetSocketAddress> socketAddressSet) {
        RpcReferenceFactoryBean.socketAddressSet = socketAddressSet;
    }

    public RegisterCenter getRegisterCenter() {
        return registerCenter;
    }

    public void setRegisterCenter(RegisterCenter registerCenter) {
        this.registerCenter = registerCenter;
    }

    public Class<?> getTargetInterface() {
        return targetInterface;
    }

    public void setTargetInterface(Class<?> targetInterface) {
        this.targetInterface = targetInterface;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getLoadBalanceStrategy() {
        return loadBalanceStrategy;
    }

    public void setLoadBalanceStrategy(String loadBalanceStrategy) {
        this.loadBalanceStrategy = loadBalanceStrategy;
    }
}
