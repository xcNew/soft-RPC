package softrpc.framework.provider;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import softrpc.framework.serialization.common.SerializerType;
import softrpc.framework.serialization.message.RequestMessage;
import softrpc.framework.utils.PropertyConfigUtil;

/**
 * Netty传输服务端
 *
 * @author xctian
 * @date 2019/12/14
 */
public class NettyServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyServer.class);

    /**
     *  服务端boss线程组，负责新连接的监听和接受
     */
    private EventLoopGroup bossGroup;

    /**
     *  服务端worker线程组，负责执行Handler中的业务处理，如数据的输入输出
     */
    private EventLoopGroup workerGroup;

    /**
     *  绑定端口的Channel
     */
    private Channel channel;

    /**
     *  netty服务端的启动
     */
    public void startServer(final int port){
        if (bossGroup != null || workerGroup != null){
            return;
        }
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        final SerializerType serialize = PropertyConfigUtil.getServerSerializer();
        serverBootstrap
                .group(bossGroup,workerGroup)
                .channel(NioServerSocketChannel.class)
                // 服务端将不能处理的客户端连接请求放在队列中等待处理，backlog参数指定了队列的大小
                .option(ChannelOption.SO_BACKLOG,1024)
                // 心跳包检测机制，设置该选项以后，连接会测试链接的状态
                .childOption(ChannelOption.SO_KEEPALIVE,true)
                // 禁用Nagle算法，使用于小数据即时传输
                .childOption(ChannelOption.TCP_NODELAY,true)
                .handler(new LoggingHandler(LogLevel.INFO))
                // 装配子通道的handler流水线。泛型参数代表需要初始化的通道类型
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel serverSocketChannel) throws Exception {
                        // 向channelPipline注册消息解码器
                        serverSocketChannel.pipeline().addLast(new NettyDecodeHandler(RequestMessage.class));
                        // 向channelPipline注册消息编码器
                        serverSocketChannel.pipeline().addLast(new NettyEncodeHandler(serialize));
                        // 向channelPipline注册业务逻辑处理器
                        serverSocketChannel.pipeline().addLast(new NettyServerHandler());
                    }
                });
        try {
            channel = serverBootstrap.bind(port).sync().channel();
        }catch (InterruptedException e){
            throw new RuntimeException(e);
        }
    }
}
