package softrpc.framework.zookeeper;

import softrpc.framework.zookeeper.message.InvokerRegisterMessage;
import softrpc.framework.zookeeper.message.ProviderRegisterMessage;

import java.util.List;
import java.util.Map;

/**
 * 消费端注册中心接口
 *
 * @author xctian
 * @date 2019/12/18
 */
public interface RegisterCenter4Invoker {

    /**
     * 获取本地服务列表
     *  key:服务的AppName+ServicePath，value:该服务下注册的提供者列表
     */
    Map<String,List<ProviderRegisterMessage>> getProviderMap();

    /**
     * 注册服务消费者到Zookeeper,返回该服务下的Provider列表
     */
    List<ProviderRegisterMessage> registerInvoker(InvokerRegisterMessage invokerRegisterMessage);
}
