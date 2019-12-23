package softrpc.framework.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import softrpc.framework.serialization.common.SerializerType;

import java.io.InputStream;
import java.util.Properties;

/**
 * 配置文件
 *
 * @author xctian
 * @date 2019/12/15
 */
public class PropertyConfigUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyConfigUtil.class);

    private static final String PROPERTY_CLASSPATH = "/soft-rpc.properties";
    private static final Properties properties = new Properties();
    /* 必须手动设置的配置项，没有提供默认值 */
    // ZK服务的地址
    private static String zkService = "";
    // 服务注册应用名
    private static  String appName4Server = "";
    // 消费方引用应用名
    private static  String appName4Client = "";

    /* 有默认值的配置项 */
    // ZK session超时时间
    private static int zkSessionTimeout;
    // ZK connect超时时间
    private static int zkConnectTimeout;
    // ChannelPool大小
    private static int channelPoolSize;
    // 客户端调用rpc服务线程池的大小
    private static int threadWorkerSize;
    // 默认的负载均衡策略
    private static String defaultClusterStrategy;
    // 服务端序列化协议
    private static SerializerType serverSerializer;
    // 客户端序列化协议
    private static String clientSerializer;

    /**

     * 初始化
     */
    static {
        InputStream is = PropertyConfigUtil.class.getResourceAsStream(PROPERTY_CLASSPATH);
        if (null == is) {
            throw new IllegalStateException("soft-rpc.properties cannot be found in the classpath");
        }
        try {
            // load方法其实就是传进去一个输入流，字节流或者字符流，字节流利用InputStreamReader转化为字符流，然后字符流用
            // BufferedReader包装，BufferedReader读取properties配置文件，每次读取一行，分割成两个字符串,因为Properties是
            // Map的子类，然后用put将两个字符串装进Properties对象。
            properties.load(is);
            zkService = properties.getProperty("soft.rpc.zookeeper.address");
            appName4Server = properties.getProperty("soft.rpc.server.app.name");
            appName4Client = properties.getProperty("soft.rpc.client.app.name");
            serverSerializer = SerializerType.getByType(properties.getProperty("soft.rpc.server.serializer", "Default"));
            zkSessionTimeout = Integer.parseInt(properties.getProperty("soft.rpc.zookeeper.session.timeout","500"));
            zkConnectTimeout = Integer.parseInt(properties.getProperty("soft.rpc.zookeeper.connection.timeout","500"));
            channelPoolSize = Integer.parseInt(properties.getProperty("soft.rpc.client.channelPoolSize","10"));
            threadWorkerSize = Integer.parseInt(properties.getProperty("soft.rpc.client.threadWorkers","10"));
            defaultClusterStrategy = properties.getProperty("soft.rpc.client.clusterStrategy.default","random");
            clientSerializer = properties.getProperty("soft.rpc.client.serializer","Default");
        } catch (Throwable throwable) {
            LOGGER.warn("配置文件加载失败",throwable);
            throw new RuntimeException(throwable);
        }finally {
            if(null != is){
                try {
                    is.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getPropertyClasspath() {
        return PROPERTY_CLASSPATH;
    }

    public static Properties getProperties() {
        return properties;
    }

    public static String getZkService() {
        return zkService;
    }

    public static void setZkService(String zkService) {
        PropertyConfigUtil.zkService = zkService;
    }

    public static String getAppName4Server() {
        return appName4Server;
    }

    public static String getAppName4Client() {
        return appName4Client;
    }

    public static int getZkSessionTimeout() {
        return zkSessionTimeout;
    }

    public static void setZkSessionTimeout(int zkSessionTimeout) {
        PropertyConfigUtil.zkSessionTimeout = zkSessionTimeout;
    }

    public static int getZkConnectTimeout() {
        return zkConnectTimeout;
    }

    public static void setZkConnectTimeout(int zkConnectTimeout) {
        PropertyConfigUtil.zkConnectTimeout = zkConnectTimeout;
    }

    public static int getChannelPoolSize() {
        return channelPoolSize;
    }

    public static void setChannelPoolSize(int channelPoolSize) {
        PropertyConfigUtil.channelPoolSize = channelPoolSize;
    }

    public static int getThreadWorkerSize() {
        return threadWorkerSize;
    }

    public static void setThreadWorkerSize(int threadWorkerSize) {
        PropertyConfigUtil.threadWorkerSize = threadWorkerSize;
    }

    public static String getDefaultClusterStrategy() {
        return defaultClusterStrategy;
    }

    public static void setDefaultClusterStrategy(String defaultClusterStrategy) {
        PropertyConfigUtil.defaultClusterStrategy = defaultClusterStrategy;
    }

    public static SerializerType getServerSerializer() {
        return serverSerializer;
    }

    public static void setServerSerializer(SerializerType serverSerializer) {
        PropertyConfigUtil.serverSerializer = serverSerializer;
    }

    public static String getClientSerializer() {
        return clientSerializer;
    }

    public static void setClientSerializer(String clientSerializer) {
        PropertyConfigUtil.clientSerializer = clientSerializer;
    }

}
