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
    JDKSerializer("Default",0),

    /**
     *  Hessian
     */
    HessianSerianlizer("Hessian",1),

    /**
     *  ProtoStuff
     */
    ProtoStuffSerializer("ProtoStuff",2);

    private String serializeName;
    private int serializeCode;

    public String getSerializeName() {
        return serializeName;
    }

    public int getSerializeCode() {
        return serializeCode;
    }

    SerializerType(String serializeName){
        this.serializeName = serializeName;
    }

    SerializerType(String serializeName,int serializeCode){
        this.serializeName = serializeName;
        this.serializeCode = serializeCode;
    }


    public static SerializerType getByType(String serializeName){
        if (serializeName.isEmpty()) {
            return JDKSerializer;
        }
        for (SerializerType serialize : SerializerType.values()){
            if(serializeName.equalsIgnoreCase(serialize.getSerializeName())){
                return serialize;
            }
        }
        // 如果传入的type都不匹配则返回JDK默认序列化工具
        return JDKSerializer;
    }

    public static SerializerType getByCode(int serializeCode){
        switch (serializeCode){
            case 1:
                return HessianSerianlizer;
            case 2:
                return ProtoStuffSerializer;
            default:
                return JDKSerializer;
        }
    }

}
