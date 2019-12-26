package softrpc.framework.loadBalance.strategy.impl;

import org.apache.commons.lang3.RandomUtils;
import softrpc.framework.loadBalance.strategy.LoadBalanceStategy;
import softrpc.framework.zookeeper.message.ProviderRegisterMessage;

import java.util.List;

/**
 * 软负载随机算法实现
 *
 * @author xctian
 * @date 2019/12/26
 */
public class RandomLoadBalanceStrategyImpl implements LoadBalanceStategy {

    @Override
    public ProviderRegisterMessage select(List<ProviderRegisterMessage> providerRegisterMessages) {
        int index = RandomUtils.nextInt(0, providerRegisterMessages.size());
        return providerRegisterMessages.get(index);
    }
}
