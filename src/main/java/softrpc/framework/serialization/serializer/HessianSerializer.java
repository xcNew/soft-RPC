package softrpc.framework.serialization.serializer;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Hessian序列化工具，要求序列化对象必须实现Serializable接口
 *
 * @author xctian
 * @date 2019/11/9
 */
public class HessianSerializer implements Serializer {
    @Override
    public <T> byte[] serialize(T t) {
        if (null == t){
            throw new NullPointerException();
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        HessianOutput hessianOutput = new HessianOutput(byteArrayOutputStream);
        try {
            hessianOutput.writeObject(t);
            hessianOutput.close();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        if(null ==data){
            throw new NullPointerException();
        }

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        HessianInput hessianInput = new HessianInput(byteArrayInputStream);
        try {
            return (T)hessianInput.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
