package softrpc.framework.serialization.serializer;

/**
 * 序列化/反序列化通用接口
 *
 * @author xctian
 * @date 2019/11/9
 */
public interface Serializer {
    /**
     * 序列化
     *
     * @param t 序列化的对象
     * @return 序列化后的byte数组
     */
    public <T> byte[] serialize(T t);

    /**
     * 反序列化
     * 
     * @param data 序列化的byte数组
     * @param clazz 指定的反序列化后的Class对象
     * @return 反序列化后的对象实例
     */
    public <T> T deserialize(byte[] data,Class<T> clazz);
}
