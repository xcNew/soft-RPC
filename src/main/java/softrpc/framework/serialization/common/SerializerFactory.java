package softrpc.framework.serialization.common;

import softrpc.framework.serialization.serializer.HessianSerializer;
import softrpc.framework.serialization.serializer.JDKSerializer;
import softrpc.framework.serialization.serializer.ProtoStuffSerializer;
import softrpc.framework.serialization.serializer.Serializer;

/**
 * Serializer简单工厂，提供根据序列化协议名称获得Serializer对象实例的功能
 *
 * @author xctian
 * @date 2019/11/10
 */
public class SerializerFactory {
    public static Serializer getSerializer(String serializerName){
        String validName = serializerName.toLowerCase();
        switch (validName){
            case "hessian": return new HessianSerializer();
            case "protostuff": return new ProtoStuffSerializer();
            default: return  new JDKSerializer();
        }
    }
}
