package softrpc.framework.zookeeper;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.SerializableSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import softrpc.framework.utils.PropertyConfigUtil;
import softrpc.framework.zookeeper.message.InvokerRegisterMessage;
import softrpc.framework.zookeeper.message.ProviderRegisterMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author xctian
 * @date 2019/12/18
 */
public class RegisterCenter implements RegisterCenter4Governance, RegisterCenter4Invoker, RegisterCenter4Provider {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterCenter.class);

    /**
     * 饿汉式单例模式
     */
    private static final RegisterCenter INSTANCE = new RegisterCenter();

    /**
     * 缓存的服务地址列表。key:服务的AppName+ServicePath，value:该服务下注册的提供者列表
     */
    private static final Map<String, List<ProviderRegisterMessage>> PROVIDER_MAP = Maps.newConcurrentMap();

    /**
     * 缓存的消费者地址列表。key:服务的AppName+ServicePath，value:该服务下注册的消费者列表
     */
    private static final Map<String, List<InvokerRegisterMessage>> INVOKER_MAP = Maps.newConcurrentMap();

    /**
     * 缓存的服务地址列表。key:服务的AppName+ServicePath，value:该服务下注册的消费者列表
     */
    private static Set<String> inovokerNodeListenerSet = Sets.newConcurrentHashSet();

    /**
     * Zookeeper地址
     */
    private static final String ZK_SERVICE = PropertyConfigUtil.getZkService();

    /**
     * Zookeeper session超时时间
     * Session是指当Client创建一个同Server的连接时产生的会话。连接Connected之后Session状态就开启，
     * Zookeeper服务器和Client采用长连接方式（Client会不停地向Server发送心跳）保证session在不出现
     * 网络问题、服务器宕机或Client宕机情况下可以一直存在。因此，在正常情况下，session会一直有效，
     * 并且ZK集群上所有机器都会保存这个Session信息。
     */
    private static final int ZK_SESSION_TIME_OUT = PropertyConfigUtil.getZkSessionTimeout();

    /**
     * Zookeeper 连接超时时间
     */
    private static final int ZK_CONNECT_TIME_OUT = PropertyConfigUtil.getZkConnectTimeout();

    /**
     * 服务注册使用的根节点路径
     */
    private static final String ROOT_PATH = "/zookeeper/soft-rpc";

    /**
     * 每个服务下标识服务提供者的父节点名字
     */
    private static final String PROVIDER_TYPE = "provider";

    /**
     * 每个服务下标识服务消费者的父节点名字
     */
    private static final String INVOKER_TYPE = "invoker";

    /**
     * ZK客户端
     */
    private static final ZkClient zkClient = new ZkClient(ZK_SERVICE, ZK_SESSION_TIME_OUT, ZK_CONNECT_TIME_OUT, new SerializableSerializer());

    private RegisterCenter() {
    }

    public static RegisterCenter getInstance() {
        return INSTANCE;
    }

    /**
     * 类初始化时注册根路径至zk
     */
    static {
        boolean exist = zkClient.exists(ROOT_PATH);
        if (!exist) {
            // 创建永久结点
            zkClient.createPersistent(ROOT_PATH, true);
        }
    }

    /**
     * 注册单个invoker
     *
     * @param invoker 待注册的invoker信息
     * @return 该服务下的Provider列表
     */
    @Override
    public List<ProviderRegisterMessage> registerInvoker(InvokerRegisterMessage invoker) {
        long startTime = System.currentTimeMillis();
        List<ProviderRegisterMessage> providerRegisterMessages = null;
        // 创建服务接口的命名空间
        final String nameSpace = invoker.getAppName() + "/" + invoker.getServicePath();
        // 对RegisterCenter加锁的原因是避免注册provider的同时注册有其他线程注册invoker，导致不同步
        synchronized (RegisterCenter.class) {
            // 创建invoker命名空间（持久结点）
            String invokerPath = ROOT_PATH + "/" + nameSpace + "/" + INVOKER_TYPE;
            boolean exist = zkClient.exists(invokerPath);
            if (!exist) {
                zkClient.createPersistent(invokerPath, true);
            }
            // 注册invoker信息结点(临时结点）,直接将InvokerRegisterMessage转换为Jason拼接invokerPath后作为结点信息注册至ZK
            String invokerNode = invokerPath + "/" + JSON.toJSONString(invoker);
            exist = zkClient.exists(invokerNode);
            if (!exist) {
                zkClient.createEphemeral(invokerNode);
            }
            // 获取服务结点
            final String servicePath = ROOT_PATH + "/" + nameSpace + "/" + PROVIDER_TYPE;
            // 若本地providermap没有该key，即该服务没有添加过任何provider，则该接口是第一次添加引用，需要为该接口注册监听器
            if (null == PROVIDER_MAP.get(nameSpace)) {
                // 为服务注册监听，实现服务自动发现(监听的是servicePath下的子节点）
                zkClient.subscribeChildChanges(servicePath, new IZkChildListener() {
                    /**
                     * IZKChildListener事件说明针对于下面三个事件触发：新增子节点/减少子节点/删除节点。注意：不监听节点内容的变化
                     *
                     * @param parentPath    监听节点全路径
                     * @param currentChilds 新的子节点列表
                     * @throws Exception
                     */
                    @Override
                    public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
                        if (null == currentChilds || currentChilds.size() == 0) {
                            PROVIDER_MAP.remove(nameSpace);
                            LOGGER.warn("[{}]节点发生变化，该结点下已无可用服务", parentPath);
                            return;
                        }
                        // 否则将更新后的子节点重新依次加载至PROVIDER_MAP
                        List<ProviderRegisterMessage> newProviderList = Lists.newArrayList();
                        for (String each : currentChilds) {
                            // 反序列化子结点成为ProviderRegisterMessage对象后存List
                            newProviderList.add(JSON.parseObject(each, ProviderRegisterMessage.class));
                        }
                        // 更新本地缓存PROVIDER_MAP
                        PROVIDER_MAP.put(nameSpace, newProviderList);
                        LOGGER.info("[{}]节点发生了变化，重加载该节点下的服务提供者信息如下：", parentPath);
                        System.out.println(newProviderList);
                    }
                });
            }
            // 否则直接获取该服务结点下的所有临时结点，即provider
            List<String> providers = zkClient.getChildren(servicePath);
            // 将结点内容从json string还原成ProviderRegisterMessage以便存入list
            providerRegisterMessages = new ArrayList<>();
            for (String each : providers) {
                providerRegisterMessages.add(JSON.parseObject(each, ProviderRegisterMessage.class));
            }
            // 注册信息缓存至本地Map
            PROVIDER_MAP.put(nameSpace, providerRegisterMessages);


            long duration = System.currentTimeMillis() - startTime;
            LOGGER.info("获取provider列表耗时{}ms:[{}]", duration, nameSpace);
            return providerRegisterMessages;
        }
    }

    /**
     * 注册单个Provider
     *
     * @param provider 需要注册的provider
     */
    @Override
    public void registerProvider(ProviderRegisterMessage provider) {
        long startTime = System.currentTimeMillis();
        // 服务接口命名空间
        final String nameSpace = provider.getAppName() + "/" + provider.getServicePath();
        // 对RegisterCenter加锁的原因是避免注册provider的同时注册有其他线程注册invoker，导致不同步
        synchronized (RegisterCenter.class) {
            // ROOT_PATH/应用名/接口限定名/provider，创建永久节点
            String providerPath = ROOT_PATH + "/" + nameSpace + "/" + PROVIDER_TYPE;
            if (!zkClient.exists(providerPath)) {
                zkClient.createPersistent(providerPath, true);
            }
            // 注册provider（临时节点)，并创建
            String serviceNode = providerPath + "/" + JSON.toJSONString(provider);
            if (!zkClient.exists(serviceNode)) {
                zkClient.createEphemeral(serviceNode);
            }
            String invokerPath = ROOT_PATH + "/" + nameSpace + "/" + INVOKER_TYPE;
            if (!zkClient.exists(invokerPath)) {
                zkClient.createPersistent(invokerPath);
            }
            boolean firstAdd = inovokerNodeListenerSet.add(invokerPath);
            if (firstAdd) {
                zkClient.subscribeChildChanges(invokerPath, new IZkChildListener() {
                    @Override
                    public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
                        if (null == currentChilds || currentChilds.size() == 0) {
                            INVOKER_MAP.remove(nameSpace);
                            LOGGER.warn("[{}]节点发生了变化，该服务节点下已无调用者", parentPath);
                            return;
                        }
                        // 反序列化还原invoker，然后更新invoker map
                        List<InvokerRegisterMessage> newInvokerList = new ArrayList<>();
                        for (String each : currentChilds) {
                            newInvokerList.add(JSON.parseObject(each, InvokerRegisterMessage.class));
                        }
                        INVOKER_MAP.put(nameSpace, newInvokerList);
                        LOGGER.info("[{}]节点发生变化，重新加载该节点下的所有invoker如下：", parentPath);
                        System.out.println(newInvokerList);
                    }
                });
            }
        }
        long duration = System.currentTimeMillis() - startTime;
        LOGGER.info("注册服务耗时{}ms[服务路径为：/zookeeper/{}/{}]", duration, nameSpace, provider.getRefId());

    }

    @Override
    public Map<String, List<InvokerRegisterMessage>> getInvokersOfProvider() {
        return INVOKER_MAP;
    }

    @Override
    public Map<String, List<ProviderRegisterMessage>> getProviderMap() {
        return PROVIDER_MAP;
    }

    @Override
    public Map<String, List<String>> getProvidersOfInvoker() {
        // TODO
        return null;
    }


}
