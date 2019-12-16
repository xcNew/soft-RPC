package softrpc.framework.serialization.message;

import java.io.Serializable;

/**
 * 自定义响应消息类
 *
 * @author xctian
 * @date 2019/12/16
 */
public class ResponseMessage implements Serializable {

    /**
     * 用于唯一标识消息的id，RequestMessage里也有相同字段
     */
    private String messageId;

    /**
     * 方法执行结果
     */
    private Object resultValue;

    /**
     * 响应超时时间
     */
    private long timeout;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Object getResultValue() {
        return resultValue;
    }

    public void setResultValue(Object resultValue) {
        this.resultValue = resultValue;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public String toString() {
        return "ResponseMessage{" +
                "messageId='" + messageId + '\'' +
                ", resultValue=" + resultValue +
                ", timeout=" + timeout +
                '}';
    }
}
