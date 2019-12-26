package softrpc.framework.loadBalance.strategy.impl;

import softrpc.framework.loadBalance.common.LoadBalanceEngine;
import softrpc.framework.loadBalance.strategy.LoadBalanceStategy;
import softrpc.framework.zookeeper.message.ProviderRegisterMessage;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 软负载加权轮询算法实现
 *
 * @author xctian
 * @date 2019/12/27
 */
public class WeightPollingLoadBalanceStrategyImpl implements LoadBalanceStategy {

    /**
     * 计数器
     */
    private int index = 0;
    /**
     * 计数器锁
     */
    private Lock lock = new ReentrantLock();

    @Override
    public ProviderRegisterMessage select(List<ProviderRegisterMessage> providerRegisterMessages) {
        ProviderRegisterMessage providerRegisterMessage = null;
        try {
            lock.tryLock(10, TimeUnit.MILLISECONDS);
            List<Integer> indexList = LoadBalanceEngine.getIndexListByWeight(providerRegisterMessages);
            if (index >= indexList.size()) {
                index = 0;
            }
            providerRegisterMessage = providerRegisterMessages.get(indexList.get(index));
            index++;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        // 兜底策略：如果获取失败则使用随机负载均衡算法选取一个
        return null == providerRegisterMessage ? new RandomLoadBalanceStrategyImpl().select(providerRegisterMessages) : providerRegisterMessage;
    }
}
