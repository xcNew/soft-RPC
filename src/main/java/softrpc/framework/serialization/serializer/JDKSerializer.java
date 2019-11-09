package softrpc.framework.serialization.serializer;

import java.io.*;

/**
 * JDK默认的序列化工具，需要被序列化的对象实现Serializable接口
 *
 * @author xctian
 * @date 2019/11/9
 */
public class JDKSerializer implements Serializer{

    @Override
    public <T> byte[] serialize(T t) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            // 通过ByteArrayOutputStream构建对象输出流ObjectOutputStream
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(t);
            objectOutputStream.close();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        try {
            // 通过ByteArrayInputStream构建对象输入流ObjectInputStream
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            return (T) objectInputStream.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
