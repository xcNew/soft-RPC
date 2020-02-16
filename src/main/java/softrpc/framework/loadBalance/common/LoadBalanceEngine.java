package softrpc.framework.loadBalance.common;

import com.google.common.collect.Maps;
import softrpc.framework.loadBalance.strategy.LoadBalanceStategy;
import softrpc.framework.loadBalance.strategy.impl.*;
import softrpc.framework.zookeeper.message.ProviderRegisterMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author xctian
 * @date 2019/12/26
 */
public class LoadBalanceEngine {

    /**
     * 缓存的负载均衡接口实现类对象Map，相当于存储了实现类的单例
     */
    private static final Map<LoadBalanceStrategyEnum, LoadBalanceStategy> STATEGY_MAP = Maps.newConcurrentMap();

    // 饿汉单例
    static {
        STATEGY_MAP.put(LoadBalanceStrategyEnum.Random, new RandomLoadBalanceStrategyImpl());
        STATEGY_MAP.put(LoadBalanceStrategyEnum.Polling, new PollingLoadBalanceStrategyImpl());
        STATEGY_MAP.put(LoadBalanceStrategyEnum.WeightPolling, new WeightPollingLoadBalanceStrategyImpl());
        STATEGY_MAP.put(LoadBalanceStrategyEnum.WeightRandom, new WeightRandomLoadBalanceStrategyImpl());
        STATEGY_MAP.put(LoadBalanceStrategyEnum.Hash, new HashLoadBalanceStrategyImpl());
    }

    public static ProviderRegisterMessage select(List<ProviderRegisterMessage> providerRegisterMessages,String loadBalanceStrategy){
        if(null == providerRegisterMessages || providerRegisterMessages.size() == 0){
            return null;
        }else if(providerRegisterMessages.size() == 1){
            return providerRegisterMessages.get(0);
        }
        LoadBalanceStrategyEnum loadBalanceStrategyEnum = LoadBalanceStrategyEnum.queryByCode(loadBalanceStrategy);
        if(null != loadBalanceStrategyEnum){
            return STATEGY_MAP.get(loadBalanceStrategyEnum).select(providerRegisterMessages);
        }else{
            return STATEGY_MAP.get(LoadBalanceStrategyEnum.Random).select(providerRegisterMessages);
        }
    }

    /*以下是通用方法，相当于工具类方法*/

    /**
     * 根据权重，获取服务地址的索引列表（服务的权重为多少，就会往列表中添加几次服务地址索引的值)
     *
     * @param providerRegisterMessages 服务地址列表
     * @return 索引列表
     */
    public static List<Integer> getIndexListByWeight(List<ProviderRegisterMessage> providerRegisterMessages) {
        if (null == providerRegisterMessages || providerRegisterMessages.size() == 0) {
            return null;
        }
        ArrayList<Integer> list = new ArrayList<>();
        int index = 0;
        for (ProviderRegisterMessage each : providerRegisterMessages) {
            int weight = each.getWeight();
            while (weight-- > 0) {
                list.add(index);
            }
            index++;
        }
        return list;
    }
}
