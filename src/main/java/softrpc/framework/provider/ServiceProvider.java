package softrpc.framework.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import softrpc.framework.serialization.message.RequestMessage;
import softrpc.framework.serialization.message.ResponseMessage;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 利用反射调用服务
 *
 * @author xctian
 * @date 2019/12/16
 */
public class ServiceProvider {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServiceProvider.class);

    public static final ApplicationContext CONTEXT = new ClassPathXmlApplicationContext("rpc-service.xml");

    public static ResponseMessage excuteMethodFromRequestMessage(RequestMessage requestMessage) throws ClassNotFoundException {
        // IOC容器中获取接口的实现类
        Object provider = CONTEXT.getBean(requestMessage.getRefId());
        // 确定方法形参类型列表，用于获取Method对象
        Class<?>[] parameterClasses = null;
        // parameterTypes已经封装在client发送至服务端的RequestMessage中
        String[] parameterTypes = requestMessage.getParameterTypes();
        if (null != parameterTypes && parameterTypes.length > 0){
            parameterClasses = new Class[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++){
                try {
                    //使用反射，根据Class的全限定名拿到对应的Class对象
                    parameterClasses[i] = Class.forName(parameterTypes[i]);
                } catch (ClassNotFoundException e) {
                    LOGGER.error("未找到该参数的类型："+parameterTypes[i]);
                    e.printStackTrace();
                    throw new ClassNotFoundException();
                }

            }
        }
        try {
            // 根据反射获取一个类对象中的Method。该方法的第一个参数name是要获得方法的名字，第二个参数parameterTypes是按声明顺序标识该方法形参类型。
            Method method = provider.getClass().getMethod(requestMessage.getMethodName(),parameterClasses);
            // 执行服务端本地方法后的返回结果,invoke第一个参数为类的实例，第二个参数为传入的对应参数的值
            Object resultValue = method.invoke(provider,requestMessage.getParameters());
            ResponseMessage responseMessage = new ResponseMessage();
            responseMessage.setMessageId(requestMessage.getMessageId());
            responseMessage.setResultValue(resultValue);
            responseMessage.setTimeout(requestMessage.getTimeout());
            return responseMessage;
        } catch (NoSuchMethodException e) {
            LOGGER.error("该方法不存在：" + requestMessage.getMethodName());
            throw new RuntimeException("反射调用产生错误");
        } catch (IllegalAccessException e) {
            LOGGER.error("无法访问该方法：" + requestMessage.getMethodName());
            throw new RuntimeException("反射调用产生错误");
        } catch (InvocationTargetException e) {
            throw new RuntimeException("反射调用产生错误");
        }
    }
}
