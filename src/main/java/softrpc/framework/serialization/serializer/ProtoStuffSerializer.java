package softrpc.framework.serialization.serializer;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于protostuff的序列化和反序列化
 *
 * @author xctian
 * @date 2019/11/9
 */
public class ProtoStuffSerializer implements Serializer {
    /**
     * schema中包含了对象进行序列化和反序列化的逻辑
     */
    private static Map<Class<?>, Schema<?>> cachedSchema = new ConcurrentHashMap<>();
    private static Objenesis objenesis = new ObjenesisStd(true);

    /**
     * 获取/构造Schema
     *
     * @param cls Class
     * @return Schema实例
     */
    @SuppressWarnings("unchecked")
    private static <T> Schema<T> getSchema(Class<T> cls){
        Schema<T> schema = (Schema<T>) cachedSchema.get(cls);
        // 构造schema
        if(null == schema){
            schema = RuntimeSchema.createFrom(cls);
            cachedSchema.put(cls,schema);
        }
        return schema;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> byte[] serialize(T t) {
        Class<T> cls = (Class<T>) t.getClass();
        //通过对象的类构建对应的schema
        Schema<T> schema = getSchema(cls);
        //使用LinkedBuffer分配一块默认大小的buffer空间
        LinkedBuffer linkedBuffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            //使用给定的schema将对象序列化为一个byte数组，并返回
            byte[] res = ProtostuffIOUtil.toByteArray(t, schema, linkedBuffer);
            return res;
        }finally {
            linkedBuffer.clear();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        if(null == data){
            throw new NullPointerException();
        }

        try {
            T message = (T)objenesis.newInstance(clazz);
            Schema<T> schema = getSchema(clazz);
            //使用给定的schema将byte数组和对象合并
            ProtostuffIOUtil.mergeFrom(data, message, schema);
            return message;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
