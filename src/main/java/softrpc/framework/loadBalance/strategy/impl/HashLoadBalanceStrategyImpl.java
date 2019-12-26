package softrpc.framework.loadBalance.strategy.impl;

import softrpc.framework.loadBalance.strategy.LoadBalanceStategy;
import softrpc.framework.utils.IPutil;
import softrpc.framework.zookeeper.message.ProviderRegisterMessage;

import java.util.List;

/**
 * 软负载hash算法的实现
 *
 * @author xctian
 * @date 2019/12/26
 */
public class HashLoadBalanceStrategyImpl implements LoadBalanceStategy {
    @Override
    public ProviderRegisterMessage select(List<ProviderRegisterMessage> providerRegisterMessages) {
        // 直接通过本地IP地址的hash值对服务列表大小取模，得到的结果作为结果索引
       String localIP = IPutil.localIp();
       int hashCode = localIP.hashCode();
       return providerRegisterMessages.get(hashCode % providerRegisterMessages.size());
    }
}
