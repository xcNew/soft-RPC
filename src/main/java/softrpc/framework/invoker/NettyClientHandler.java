package softrpc.framework.invoker;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import softrpc.framework.serialization.message.ResponseMessage;

/**
 * @author xctian
 * @date 2019/12/24
 */
public class NettyClientHandler extends SimpleChannelInboundHandler<ResponseMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyClientHandler.class);

    public NettyClientHandler(){};

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();;
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ResponseMessage responseMessage) throws Exception {
        // netty异步获取结果response后存入结果阻塞队列
        ResponseReceiverHolder.putResultValue(responseMessage);
        LOGGER.info("客户端接收返回结果：[content:{}]",responseMessage,responseMessage.getMessageId());
    }
}
