package softrpc.framework.invoker;

import softrpc.framework.serialization.message.ResponseMessage;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * 返回结果response的包装类，用于实现同步阻塞等待结果
 *
 * @author xctian
 * @date 2019/12/27
 */
public class ResponseReceiver {

    /**
     * 存储异步返回结果的阻塞队列
     */
    private BlockingQueue<ResponseMessage> responseQueue = new ArrayBlockingQueue<ResponseMessage>(1);
    /**
     * 用于记录异步结果返回的时刻，以便判断超时
     */
    private long responseTime;

    public boolean isExpire(){
        ResponseMessage response = responseQueue.peek();
        if(null == response){
            // 有可能是异步结果还未加入queue，也有可能是结果已经被取走
            // 但如果结果被取走，则会在Holder的Map中将其移除，此对象的isExpire方法不会被调用
            return false;
        }
        long time = response.getTimeout();
        if(System.currentTimeMillis() - responseTime > time){
            return true;
        }
        return false;
    }

    public BlockingQueue<ResponseMessage> getResponseQueue() {
        return responseQueue;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }
}
