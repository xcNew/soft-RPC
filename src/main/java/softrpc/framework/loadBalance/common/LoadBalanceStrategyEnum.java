package softrpc.framework.loadBalance.common;

import org.apache.commons.lang3.StringUtils;

/**
 * @author xctian
 * @date 2019/12/26
 */
public enum LoadBalanceStrategyEnum {

    // 随机算法
    Random("Random"),

    // 权重随机算法
    WeightRandom("WeightRandom"),

    // 权重轮询
    WeightPolling("WeightPolling"),

    // 轮询算法
    Polling("Polling"),

    // IP地址hash算法
    Hash("Hash");

    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    LoadBalanceStrategyEnum(String code) {
        this.code = code;
    }

    public static LoadBalanceStrategyEnum queryByCode(String code){
        if(null == code || StringUtils.isBlank(code)){
            return null;
        }
        for(LoadBalanceStrategyEnum strategyEnum : values()){
            if(StringUtils.equalsIgnoreCase(code,strategyEnum.getCode())){
                return strategyEnum;
            }
        }
        return null;
    }
}
