package softrpc.framework.zookeeper.message;

import java.util.UUID;

/**
 * zk的Provider结点类
 *
 * @author xctian
 * @date 2019/12/18
 */
public class InvokerRegisterMessage {

    /**
     * 接口所在应用名
     */
    private String appName;
    /**
     * 接口全限定名
     */
    private String servicePath;
    /**
     * 本机机器码，作为本机的唯一标识
     */
    private static String invokerMachineID4Client = UUID.randomUUID().toString();
    /**
     * 服务分组组名，本项目未用到，默认为default
     */
    private String groupName;
    /**
     * 服务治理时需要统计所有消费者的机器ID,此时用成员变量进行区分(在读结点时用到)
     */
    private String invokerMachineID4Server;

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

    public static String getInvokerMachineID4Client() {
        return invokerMachineID4Client;
    }

    public static void setInvokerMachineID4Client(String invokerMachineID4Client) {
        InvokerRegisterMessage.invokerMachineID4Client = invokerMachineID4Client;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getInvokerMachineID4Server() {
        return invokerMachineID4Server;
    }

    public void setInvokerMachineID4Server(String invokerMachineID4Server) {
        this.invokerMachineID4Server = invokerMachineID4Server;
    }
}
