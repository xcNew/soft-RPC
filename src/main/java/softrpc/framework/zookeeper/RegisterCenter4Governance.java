package softrpc.framework.zookeeper;

import softrpc.framework.zookeeper.message.InvokerRegisterMessage;

import java.util.List;
import java.util.Map;

/**
 * 服务治理接口
 *
 * @author xctian
 * @date 2019/12/18
 */
public interface RegisterCenter4Governance {
    /**
     * 获取当前时刻每一个服务的调用者，key：服务接口全限定名，value：服务调用者列表
     * 调用者信息在该项目里暂时只有machineID,唯一标识调用者机器
     */
    Map<String,List<InvokerRegisterMessage>> getInvokersOfProvider();

    /**
     * 获取当前时刻每一个调用者都在使用哪些服务
     * key:调用者machineID,value:服务者命名空间(appName+servicePath)列表
     */
    Map<String,List<String>> getProvidersOfInvoker();
}
