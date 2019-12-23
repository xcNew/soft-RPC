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

    ;

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
                channel = registerChannel(socketAddress);
            }
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
}
