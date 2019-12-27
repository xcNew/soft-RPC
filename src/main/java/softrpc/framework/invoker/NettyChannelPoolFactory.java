package softrpc.framework.invoker;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import softrpc.framework.provider.NettyDecodeHandler;
import softrpc.framework.provider.NettyEncodeHandler;
import softrpc.framework.serialization.common.SerializerType;
import softrpc.framework.serialization.message.ResponseMessage;
import softrpc.framework.utils.PropertyConfigUtil;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

/**
 * 客户端ChannelPool工厂
 *
 * @author xctian
 * @date 2019/12/23
 */
public class NettyChannelPoolFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyChannelPoolFactory.class);

    /**
     * 饿汉单例模式
     */
    private static final NettyChannelPoolFactory INSTANCE = new NettyChannelPoolFactory();
    /**
     * 缓存ChannelPool的Map:Key是服务地址，value是存放这个地址对应的Channel阻塞队列
     */
    private static final Map<InetSocketAddress, ArrayBlockingQueue<Channel>> CHANNEL_POOL_MAP = Maps.newConcurrentMap();
    /**
     * 每个服务地址ChannelPool的Channel数量，可在soft-rpc.properties中进行配置
     */
    private static final int CHANNEL_POOL_SIZE = PropertyConfigUtil.getChannelPoolSize();

    private NettyChannelPoolFactory() {
    }

    public static NettyChannelPoolFactory getInstance() {
        return INSTANCE;
    }

    /**
     * 为服务地址创建ChannelPool并缓存到Map中
     *
     * @param socketAddress 服务地址
     */
    public void registerChannelQueueToMap(InetSocketAddress socketAddress) {
        long startTime = System.currentTimeMillis();
        int existedChannel = 0;
        while (existedChannel < CHANNEL_POOL_SIZE) {
            Channel channel = null;
            while (null == channel) {
                // 创建失败则重试
                channel = registerChannel(socketAddress);
            }
            existedChannel++;
            // 将创建后的channel加入对应的阻塞队列
            ArrayBlockingQueue<Channel> channelArrayBlockingQueue = CHANNEL_POOL_MAP.get(socketAddress);
            if (null == channelArrayBlockingQueue) {
                channelArrayBlockingQueue = new ArrayBlockingQueue<Channel>(CHANNEL_POOL_SIZE);
                CHANNEL_POOL_MAP.put(socketAddress, channelArrayBlockingQueue);
            }
            // offer方法当队列满，而且放入时间超过设定时间时，返回false;
            //  put方法当队列满时，会调用wait方法，put方法会等待一个空的位置出来，然后再执行insert
            channelArrayBlockingQueue.offer(channel);
            long duation = System.currentTimeMillis() - startTime;
            LOGGER.info("创建channelPool耗时{}ms:[{}:{}]", duation, socketAddress.getHostName(), socketAddress.getPort());
        }
    }

    /**
     * 基于netty为服务地址创建Channel
     *
     * @param socketAddress
     * @return 创建过后的Channel
     */
    public Channel registerChannel(InetSocketAddress socketAddress) {
        try {
            EventLoopGroup group = new NioEventLoopGroup(10);
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.remoteAddress(socketAddress);
            final String serializer = PropertyConfigUtil.getClientSerializer();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    // handler在初始化时就会执行，而childHandler会在客户端成功connect后才执行，这是两者的区别。
                    .handler(new ChannelInitializer<SocketChannel>() {
                        // 父通道调用initChannel方法时，会将新接收到的Channel作为参数传递给initChannel方法
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new NettyEncodeHandler(SerializerType.getByType(serializer)));
                            ch.pipeline().addLast(new NettyDecodeHandler(ResponseMessage.class));
                            ch.pipeline().addLast(new NettyClientHandler());
                        }
                    });

            ChannelFuture channelFuture = bootstrap.connect().sync();
            final Channel newChannel = channelFuture.channel();
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            final List<Boolean> isSuccessHolder = Lists.newArrayListWithCapacity(1);
            // 监听是否channel创建成功
            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (channelFuture.isSuccess()) {
                        isSuccessHolder.add(Boolean.TRUE);
                    } else {
                        channelFuture.cause().printStackTrace();
                        isSuccessHolder.add(Boolean.FALSE);
                    }
                    countDownLatch.countDown();
                }
            });
            // 阻塞等待Channel创建的结果
            countDownLatch.await();
            if (isSuccessHolder.get(0)) {
                return newChannel;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * 根据地址获取对应的ChannelPool缓存队列
     *
     * @param socketAddress
     * @return
     */
    public ArrayBlockingQueue<Channel> acquire(InetSocketAddress socketAddress) {
        ArrayBlockingQueue<Channel> arrayBlockingQueue = CHANNEL_POOL_MAP.get(socketAddress);
        if (null == arrayBlockingQueue) {
            registerChannelQueueToMap(socketAddress);
            return CHANNEL_POOL_MAP.get(socketAddress);
        } else {
            return arrayBlockingQueue;
        }
    }

    /**
     * Channel使用完毕之后，回收到阻塞队列
     *
     * @param arrayBlockingQueue
     * @param channel
     * @param inetSocketAddress
     */
    public void release(ArrayBlockingQueue<Channel> arrayBlockingQueue, Channel channel, InetSocketAddress inetSocketAddress) {
        if (null == arrayBlockingQueue) {
            return;
        }
        // 回收之前判断channel是否可用，若不可用则重新注册一个放入阻塞队列
        if (null == channel || !channel.isActive() || !channel.isOpen() || !channel.isWritable()) {
            if (channel != null) {
                channel.deregister().syncUninterruptibly().awaitUninterruptibly();
                channel.closeFuture().syncUninterruptibly().awaitUninterruptibly();
            }
            Channel newChannel = null;
            while (null == newChannel) {
                newChannel = registerChannel(inetSocketAddress);
            }
            arrayBlockingQueue.offer(newChannel);
            return;
        }
        arrayBlockingQueue.offer(channel);
    }
}
