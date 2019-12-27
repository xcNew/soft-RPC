package softrpc.framework.invoker;

import com.google.common.collect.Maps;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import softrpc.framework.serialization.message.ResponseMessage;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author xctian
 * @date 2019/12/27
 */
public class ResponseReceiverHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseReceiverHolder.class);

    /**
     * 用于缓存返回结果包装类的Map,key：请求id，value：返回结果包装类
     */
    private static final Map<String,ResponseReceiver> responseMap = Maps.newConcurrentMap();
    /**
     * 清除过期结果的线程池，类似SingleThreadExecutor
     */
    private static final ExecutorService removeExpireKeyExecutor = new ThreadPoolExecutor(1,1,0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<Runnable>(1),new DefaultThreadFactory("removeExpireKeyExecutor"));
    // 删除超时未获取结果的key，防止内存泄漏
    static {
        // 线程池的execute方法没有返回对象，且只接收runnable，sumit有future<T>封装的返回对象，且还可以接收callable<T>，<T>为callable的call方法返回类型
        removeExpireKeyExecutor.execute(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        for(Map.Entry<String,ResponseReceiver> entry :responseMap.entrySet()){
                            boolean isExpired = entry.getValue().isExpire();
                            if(isExpired){
                                responseMap.remove(entry.getKey());
                            }
                            Thread.sleep(10);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * 为请求创建一个返回封装， 缓存至Map
     * @param traceId 消息id
     */
    public static void initResponseData(String traceId){
        responseMap.put(traceId,new ResponseReceiver());
    }

    /**
     * 将Netty异步返回结果放入阻塞队列
     */
    public static void putResultValue(ResponseMessage responseMessage){
        ResponseReceiver responseReceiver = responseMap.get(responseMessage.getMessageId());
        if(null == responseReceiver){
            responseReceiver = new ResponseReceiver();
            responseMap.put(responseMessage.getMessageId(),responseReceiver);
        }
        responseReceiver.setResponseTime(System.currentTimeMillis());
        responseReceiver.getResponseQueue().add(responseMessage);
    }

    /**
     * 从阻塞队列获取netty异步返回的结果值
     */
    public static ResponseMessage getValue(String messageId,long timeout) throws InterruptedException{
        ResponseReceiver responseReceiver = responseMap.get(messageId);
        try {
            // 阻塞等待队列有值然后取出，等待超时时间为timeout，超时则产生中断异常
            return responseReceiver.getResponseQueue().poll(timeout,TimeUnit.MILLISECONDS);
        }catch (InterruptedException e){
            LOGGER.error("结果队列取值超时，线程中断！");
            throw new InterruptedException();
        }finally {
            // 无论是否成功取到，本次请求已经结束，从缓存中移除
            responseMap.remove(messageId);
        }
    }
}
