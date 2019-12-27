package softrpc.framework.invoker;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import softrpc.framework.serialization.message.RequestMessage;
import softrpc.framework.serialization.message.ResponseMessage;

import java.net.InetSocketAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * @author xctian
 * @date 2019/12/27
 */
public class RevokerServiceCallable implements Callable<ResponseMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RevokerServiceCallable.class);
    /**
     * 服务地址
     */
    private InetSocketAddress inetSocketAddress;
    /**
     * 请求消息
     */
    private RequestMessage requestMessage;
    /**
     * 连接服务的Channel
     */
    private Channel channel;

    public RevokerServiceCallable(InetSocketAddress inetSocketAddress, RequestMessage requestMessage) {
        this.inetSocketAddress = inetSocketAddress;
        this.requestMessage = requestMessage;
    }

    @Override
    public ResponseMessage call() throws Exception {
        // 创建返回结果包装类，存入结果容器
        ResponseReceiverHolder.initResponseData(requestMessage.getMessageId());
        // 根据本地调用服务提供者地址获取对应的netty通道channel队列
        ArrayBlockingQueue<io.netty.channel.Channel> blockingQueue = NettyChannelPoolFactory.getInstance().acquire(inetSocketAddress);
        try {
            if (null == channel) {
                // 尝试从channelPool阻塞队列取出一个可用channel
                channel = blockingQueue.poll(100, TimeUnit.MILLISECONDS);
            }
            // 若无效则重新创建一个
            while (null == channel || !channel.isOpen() || !channel.isActive() || !channel.isWritable()) {
                channel = NettyChannelPoolFactory.getInstance().registerChannel(inetSocketAddress);
            }
            // 本次调用信息写入netty通道，发起异步调用,nettyServer端会相应channel变化情况
            LOGGER.info("客户端发送请求消息：[content:{} id:{}]", requestMessage, requestMessage.getMessageId());
            ChannelFuture channelFuture = channel.writeAndFlush(requestMessage);
            channelFuture.syncUninterruptibly();
            // 从结果包装类中取出结果，此时会同步阻塞，等待nettyServer处理完毕后在NettyClientHandler中将结果Put进阻塞队列
            return ResponseReceiverHolder.getValue(requestMessage.getMessageId(), requestMessage.getTimeout());
        } catch (InterruptedException e) {
            LOGGER.error("请求超时，线程中断！");
            throw new InterruptedException();
        } finally {
            // 调用完毕过后将channel释放会对应的pool
            NettyChannelPoolFactory.getInstance().release(blockingQueue, channel, inetSocketAddress);
        }
    }
}
