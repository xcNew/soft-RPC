package softrpc.framework.loadBalance.strategy;

import softrpc.framework.zookeeper.message.ProviderRegisterMessage;

import java.util.List;

/**
 * 负载均衡接口
 *
 * @author xctian
 * @date 2019/12/26
 */
public interface LoadBalanceStategy {

    /**
     * 负载均衡算法:从服务器地址列表中选取一个服务地址
     *
     * @param providerRegisterMessages 服务地址列表
     * @return 最终选取的服务地址
     */
    ProviderRegisterMessage select(List<ProviderRegisterMessage> providerRegisterMessages);
}
