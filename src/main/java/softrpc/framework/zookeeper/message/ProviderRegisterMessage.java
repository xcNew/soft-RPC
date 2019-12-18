package softrpc.framework.zookeeper.message;

import java.io.Serializable;

/**
 * zk的Provider结点类
 *
 * @author xctian
 * @date 2019/12/18
 */
public class ProviderRegisterMessage implements Serializable{

    /**
     * 接口所在应用名
     */
    private String appName;
    /**
     * 接口全限定名，用appName+servicePath唯一标识一个服务
     */
    private String servicePath;
    /**
     * 接口实现类对应的xml配置中的bean id
     */
    private String refId;
    /**
     * 服务主机地址
     */
    private String serverIp;
    /**
     * 服务端口
     */
    private int serverPort;
    /**
     * 服务端配置超时时间
     */
    private long timeout;
    /**
     * 服务端限流，默认大小是10
     */
    private int workThread;
    /**
     * 服务提供者负载均衡权重：1~100
     */
    private int weight;
    /**
     * 服务分组名，本项目未用到，默认default
     */
    private String groupName;

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

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public int getWorkThread() {
        return workThread;
    }

    public void setWorkThread(int workThread) {
        this.workThread = workThread;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
