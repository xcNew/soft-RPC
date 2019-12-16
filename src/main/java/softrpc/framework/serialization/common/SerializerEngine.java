package softrpc.framework.serialization.common;

import softrpc.framework.serialization.serializer.Serializer;

/**
 * 序列化工具引擎，通过传入参数serializerType灵活选择序列化方案,有两种实现方式
 *
 * @author xctian
 * @date 2019/11/10
 */
public class SerializerEngine {
    /**
     * 序列化（工厂模式实现方案）
     *
     * @param   t                序列化对象
     * @param   serializerType 指定的序列化协议
     * @return 序列化后的字节数组
     */
    public static <T> byte[] serialize(T t, SerializerType serializerType){
        // 通过工厂获取指定的Serializer
        Serializer serializer = SerializerFactory.getSerializer(serializerType.getSerializeName());
        return serializer.serialize(t);
    }

    /**
     * 反序列化（工厂模式实现方案）
     *
     * @param   data             序列化生成的字符数组
     * @param   clazz            反序列化后的对象类型
     * @param   serializerType  指定的序列化协议
     * @return 序列化后的字节数组
     */
    public static <T> T deserialize(byte[] data, Class<T> clazz, String serializerType){
        Serializer serializer = SerializerFactory.getSerializer(serializerType);
        return serializer.deserialize(data, clazz);
    }
}


