package softrpc.framework.serialization.message;

import java.io.Serializable;
import java.util.Arrays;

/**
 * 自定义请求消息类
 *
 * @author xctian
 * @date 2019/12/16
 */
public class RequestMessage implements Serializable {
    /**
     * 用于唯一标识消息的id
     */
    private String messageId;

    /*====以下参数可以从配置中读取====*/
    /**
     * 服务接口的全限定名
     */
    private String servicePath;
    /**
     * 响应超时时间
     */
    private long timeout = 3000;
    /*====以下参数利用反射获取====*/
    /**
     * 待执行的方法名字
     */
    private String methodName;
    /**
     * 待执行的方法参数值(实参)
     */
    private Object[] parameters;
    /**
     * 待执行的方法参数类型的全限定名，如java.lang.String
     */
    private String[] parameterTypes;

    /*====以下参数从客户端本地缓存的服务方信息中获取====*/
    /**
     * 服务端限流的信号量大小
     */
    private Integer maxWorkThread;
    /**
     * 服务方接口实现类的bean标签的id(经过负载均衡后)
     */
    private String refId;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getServicePath() {
        return servicePath;
    }

    public void setServicePath(String servicePath) {
        this.servicePath = servicePath;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public String[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(String[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Integer getMaxWorkThread() {
        return maxWorkThread;
    }

    public void setMaxWorkThread(Integer workThread) {
        this.maxWorkThread = workThread;
    }

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    @Override
    public String toString() {
        return "RequestMessage{" +
                "messageId='" + messageId + '\'' +
                ", servicePath='" + servicePath + '\'' +
                ", timeout=" + timeout +
                ", methodName='" + methodName + '\'' +
                ", parameters=" + Arrays.toString(parameters) +
                ", parameterTypes=" + Arrays.toString(parameterTypes) +
                ", workThread=" + maxWorkThread +
                ", refId='" + refId + '\'' +
                '}';
    }
}
