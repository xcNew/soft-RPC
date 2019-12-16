package softrpc.framework.provider;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import softrpc.framework.serialization.common.SerializerEngine;
import softrpc.framework.serialization.common.SerializerType;

/**
 * 编码器Handler，消息编码格式为：消息头部(序列化协议code+消息长度)+消息内容
 *
 * @author xctian
 * @date 2019/12/16
 */
public class NettyEncodeHandler extends MessageToByteEncoder {

    public static final Logger LOGGER = LoggerFactory.getLogger(NettyEncodeHandler.class);

    private SerializerType serializeType;

    /**
     * 构造编码器实例必须提供所选择的序列化协议
     */
    public NettyEncodeHandler(SerializerType serializeType){
        this.serializeType = serializeType;
    }

    /**
     * 消息编码方法，NettyServerHandler里调用write方法将response传入至参数in
     * @param channelHandlerContext
     * @param in
     * @param out
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object in, ByteBuf out) throws Exception {
        long startTime = System.currentTimeMillis();
        // 获取序列化协议code
        int serializerCode = serializeType.getSerializeCode();
        // 将其写入消息头部第一个int
        out.writeInt(serializerCode);
        // 将对象进行序列化
        byte[] data = SerializerEngine.serialize(in,serializeType);
        // 将data长度写入消息头部第二个int
        out.writeInt(data.length);
        // 将消息体写入
        out.writeBytes(data);
        long duration = System.currentTimeMillis() - startTime;
        LOGGER.info("[{}]序列化协议编码耗时{}ms",serializeType.getSerializeName(),duration);

    }
}
