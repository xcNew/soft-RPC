package softrpc.framework.serialization.common;

import com.sun.deploy.util.StringUtils;

/**
 * 支持的序列化工具枚举类,用作SerializerEngine的Map常量的key
 *
 * @author xctian
 * @date 2019/11/10
 */
public enum SerializerType {
    /**
     * JDK默认的序列化工具
     */
    JDKSerializer("Default"),

    /**
     *  Hessian
     */
    HessianSerianlizer("Hessian"),

    /**
     *  ProtoStuff
     */
    ProtoStuffSerializer("ProtoStuff");

    private String serializeType;

    public String getSerializeType() {
        return serializeType;
    }

    SerializerType(String serializeType){
        this.serializeType = serializeType;
    }


    public static SerializerType getByType(String serializeType){
        if (serializeType.isEmpty()) {
            return JDKSerializer;
        }
        for (SerializerType serialize : SerializerType.values()){
            if(serializeType.equalsIgnoreCase(serialize.getSerializeType())){
                return serialize;
            }
        }
        // 如果传入的type都不匹配则返回JDK默认序列化工具
        return JDKSerializer;
    }
}
