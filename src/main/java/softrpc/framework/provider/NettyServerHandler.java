package softrpc.framework.provider;

import com.google.common.collect.Maps;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import softrpc.framework.serialization.message.RequestMessage;
import softrpc.framework.serialization.message.ResponseMessage;

import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


/**
 * 服务端业务逻辑处理器
 *
 * SimpleChannelInboundHandler在接收到数据后会自动release掉数据占用的Bytebuffer资源(自动调用Bytebuffer.release())。
 * 而为何服务器端不能用呢，因为我们想让服务器把客户端请求的数据发送回去，而服务器端有可能在channelRead方法返回前还
 * 没有写完数据，因此不能让它自动release。
 *
 * @author xctian
 * @date 2019/12/16
 */
public class NettyServerHandler extends ChannelInboundHandlerAdapter{

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyServerHandler.class);

    /**
     * 服务端限流Map,可在rpc-service.xml中进行配置
     */
    private  static final Map<String,Semaphore> SERVICE_SEMAPHORE_MAP = Maps.newConcurrentMap();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        long startTime = System.currentTimeMillis();
        RequestMessage requestMessage = (RequestMessage)msg;
        LOGGER.info("服务端接收请求消息：[content:{}id:{}]",requestMessage,requestMessage.getMessageId());
        if(ctx.channel().isWritable()){
            long consumeTimeout = requestMessage.getTimeout();
            // 获取具体的接口实现类的bean id
            String serviceProviderId = requestMessage.getRefId();
            // 进行服务端限流
            int maxWorkThread = requestMessage.getMaxWorkThread();
            Semaphore semaphore = SERVICE_SEMAPHORE_MAP.get(serviceProviderId);
            // 双锁校验式单例
            if(null == semaphore){
                synchronized (SERVICE_SEMAPHORE_MAP){
                    //第二次判断是为了避免重复new Semaphore
                    if(null == semaphore){
                        semaphore = new Semaphore(maxWorkThread);
                        SERVICE_SEMAPHORE_MAP.put(serviceProviderId,semaphore);
                    }
                }
            }
            ResponseMessage responseMessage = null;
            boolean acquire = false;
            try {
                // 利用semaphore实现服务端限流,因为反射操作执行效率低下，如果大量反射同时执行，将占用资源
                acquire = semaphore.tryAcquire(consumeTimeout, TimeUnit.MILLISECONDS);
                if(acquire){
                    // 成功则发起反射调用，调用服务
                    responseMessage = ServiceProvider.excuteMethodFromRequestMessage(requestMessage);
                }else {
                    LOGGER.warn("服务限流，请求超时");
                }
            }catch (Exception e){
                LOGGER.error("服务方反射调用本地方法时产生错误",e);
                throw new RuntimeException("服务方反射调用本地方法时产生错误");
            }finally {
                if (acquire){
                    // 恢复semaphore
                    semaphore.release();
                }
            }
            if(null == responseMessage){
                throw new RuntimeException("服务方反射调用本地方法时产生错误");
            }
            // 将调用方法得到的responseMessage写回channel
            long duration = System.currentTimeMillis() - startTime;
            LOGGER.info("服务端调用服务耗时{}ms",duration);
            LOGGER.info("服务端方法调用返回结果：[content:{} messageId:{}]",responseMessage,responseMessage.getMessageId());
            ctx.writeAndFlush(responseMessage);
        }else {
            LOGGER.error("Channel异常，请求失败");
            throw new RuntimeException("Channel异常，请求失败");
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
