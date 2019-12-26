package softrpc.framework.loadBalance.strategy.impl;

import org.apache.commons.lang3.RandomUtils;
import softrpc.framework.loadBalance.common.LoadBalanceEngine;
import softrpc.framework.loadBalance.strategy.LoadBalanceStategy;
import softrpc.framework.zookeeper.message.ProviderRegisterMessage;

import java.util.List;

/**
 * 软负载加权随机算法实现
 *
 * @author xctian
 * @date 2019/12/27
 */
public class WeightRandomLoadBalanceStrategyImpl implements LoadBalanceStategy {

    @Override
    public ProviderRegisterMessage select(List<ProviderRegisterMessage> providerRegisterMessages) {
        //根据加权创建服务索引列表:比如权重为3，则该服务的索引在列表出现三次。建立该列表后对其进行随机查找
        List<Integer> indexList = LoadBalanceEngine.getIndexListByWeight(providerRegisterMessages);
        int index = RandomUtils.nextInt(0,indexList.size());
        return providerRegisterMessages.get(index);
    }
}
