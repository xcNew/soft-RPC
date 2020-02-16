package softrpc.framework.serialization.common;

import softrpc.framework.serialization.serializer.HessianSerializer;
import softrpc.framework.serialization.serializer.JDKSerializer;
import softrpc.framework.serialization.serializer.ProtoStuffSerializer;
import softrpc.framework.serialization.serializer.Serializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Serializer简单工厂，提供根据序列化协议名称获得Serializer对象实例的功能
 *
 * @author xctian
 * @date 2019/11/10
 */
public class SerializerFactory {

    static  Map<String,Serializer> SERIALIZER_MAP = new HashMap<>();

    static {
        SERIALIZER_MAP.put("hessian",new HessianSerializer());
        SERIALIZER_MAP.put("protostuff",new ProtoStuffSerializer());
        SERIALIZER_MAP.put("default",new JDKSerializer());
    }
    public static Serializer getSerializer(String serializerName){
        String validName = serializerName.toLowerCase();
        Serializer serializer = SERIALIZER_MAP.get(validName);
        if(serializer != null){
            return serializer;
        }else {
            return SERIALIZER_MAP.get("default");
        }
    }
}
