package softrpc.framework.spring.factoryBean;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import softrpc.framework.provider.NettyServer;
import softrpc.framework.utils.IPutil;
import softrpc.framework.zookeeper.RegisterCenter;
import softrpc.framework.zookeeper.RegisterCenter4Provider;
import softrpc.framework.zookeeper.message.ProviderRegisterMessage;

import java.util.Map;

/**
 * RpcServiceFactoryBean
 * 由于parser类的作用，对于标签的解析生成的是一个已经组装了的factoryBean对象，而factoryBean返回的实际对象是getObject里指定的类对象
 *
 * @author xctian
 * @date 2019/12/18
 */
public class RpcServiceFactoryBean implements FactoryBean, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServiceFactoryBean.class);

    /**
     * 该Map用于缓存每个NettyServer实例
     * key:已经开启服务的端口；value:对应的NettyServer，避免重复注册端口，以及便于NettyServer管理
     */
    private static final Map<Integer, NettyServer> NETTY_SERVER_MAP = Maps.newConcurrentMap();

    /**
     * 注册中心
     */
    private static final RegisterCenter4Provider registerCenter4Provider = RegisterCenter.getInstance();

   /* 以下信息注册到ZK*/
   /* 必选项 */
   /**
     * 接口所在应用名
     */
   private String appName;
    /**
     * 服务接口
     */
    private String servicePath;
    /**
     * 该服务接口实现类对象,用于查找对应的class标签内容，即实现类的全限定名
     */
    private String ref;
    /**
     * 服务端口
     */
    private Integer serverPort;
    /**
     * 服务超时时间
     */
    private Integer timeout;

    /* 以下为可选项 */
    /**
     * 服务分组组名
     */
    private String groupName = "default";
    /**
     * Provider权重：1-100
     */
    private int weight = 1;
    /**
     * 服务端限流大小
     */
    private int workThreads = 10;

    @Override
    public Object getObject() throws Exception {
        // 服务端只需要加载xml文件完成相关初始化即可，无需返回bean对象
        return null;
    }

    @Override
    public Class<?> getObjectType() {
        return Object.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 组装service标签信息,以上成员变量的值由parser类解析后已经配置完毕
        ProviderRegisterMessage provider = new ProviderRegisterMessage();
        provider.setAppName(appName);
        provider.setServicePath(servicePath);
        provider.setRefId(ref);
        // 获取本机ip
        provider.setServerIp(IPutil.localIp());
        provider.setServerPort(serverPort);
        provider.setTimeout(timeout);
        // 以下项并不强制要求标签内有内容，若标签无内容则设置为本类中定义的值
        provider.setWorkThread(workThreads);
        provider.setWeight(weight);
        provider.setGroupName(groupName);
        // 注册服务到ZK
        registerCenter4Provider.registerProvider(provider);
        // 发布代理服务
        long nowTime = System.currentTimeMillis();
        NettyServer nettyServer = NETTY_SERVER_MAP.get(serverPort);
        // 双锁校验式单例
        if(null == nettyServer){
            // 开启新的nettyServer并缓存
            synchronized (RpcServiceFactoryBean.class){
                if(null == NETTY_SERVER_MAP.get(serverPort)){
                    nettyServer = new NettyServer();
                    nettyServer.startServer(serverPort);
                    NETTY_SERVER_MAP.put(serverPort,nettyServer);
                    long duration = System.currentTimeMillis() - nowTime;
                    LOGGER.info("[{}]端口开启代理服务耗时：{}ms",serverPort,duration);
                }
            }
        }
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getServicePath() {
        return servicePath;
    }

    public void setServicePath(String servicePath) {
        this.servicePath = servicePath;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getWorkThreads() {
        return workThreads;
    }

    public void setWorkThreads(int workThreads) {
        this.workThreads = workThreads;
    }
}
