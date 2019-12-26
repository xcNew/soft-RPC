package softrpc.framework.loadBalance.strategy.impl;

import softrpc.framework.loadBalance.strategy.LoadBalanceStategy;
import softrpc.framework.zookeeper.message.ProviderRegisterMessage;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 软负载轮询算法的实现
 *
 * @author xctian
 * @date 2019/12/26
 */
public class PollingLoadBalanceStrategyImpl implements LoadBalanceStategy {

    /**
     * 计数器
     */
    private int index = 0;

    /**
     * 计数器锁
     * 并发情况下对index的改变需要加锁，以保证轮询算法的正确性
     */
    private Lock lock = new ReentrantLock();

    @Override
    public ProviderRegisterMessage select(List<ProviderRegisterMessage> providerRegisterMessages) {
        ProviderRegisterMessage providerRegisterMessage = null;
        try {
            // 尝试获取锁，10ms的超时时间
            lock.tryLock(10, TimeUnit.MILLISECONDS);
            if (index >= providerRegisterMessages.size()) {
                index = 0;
            }
            providerRegisterMessage = providerRegisterMessages.get(index);
            index++;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.lock();
        }

        // 兜底策略：如果获取失败则使用随机负载均衡算法选取一个
        return null == providerRegisterMessage ? new RandomLoadBalanceStrategyImpl().select(providerRegisterMessages) : providerRegisterMessage;
    }
}
