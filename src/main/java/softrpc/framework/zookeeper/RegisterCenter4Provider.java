package softrpc.framework.zookeeper;

import softrpc.framework.zookeeper.message.ProviderRegisterMessage;

/**
 * 服务端注册中心接口
 *
 * @author xctian
 * @date 2019/12/18
 */
public interface RegisterCenter4Provider {

    /**
     * 注册服务到Zookeeper
     */
    void registerProvider(ProviderRegisterMessage providerRegisterMessage);
}
