package softrpc.framework.provider;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import softrpc.framework.serialization.common.SerializerEngine;
import softrpc.framework.serialization.common.SerializerType;

import java.util.List;

/**
 * 解码器handler
 *
 * @author xctian
 * @date 2019/12/15
 */
public class NettyDecodeHandler extends ByteToMessageDecoder {

    public static final Logger LOGGER = LoggerFactory.getLogger(NettyDecodeHandler.class);

    /**
     * 解码类的Class对象
     */
    private Class<?> genericClass;

    /**
     * 构造解码器必须提供反序列化的对象类型
     */
    public NettyDecodeHandler(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    /**
     * 解码器重写decode方法，decode方法处理完成后，会继续后面的传递处理：将list结果列表传递到下一个InboundHandler
     * @param ctx
     * @param in
     * @param out
     * @throws Exception
     */
    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        long startTime = System.currentTimeMillis();
        // 消息头部长度8字节=序列化协议int + 消息长度int
        if(in.readableBytes() < 8){
            return;
        }
        in.markReaderIndex();
        int serializerCode = in.readInt();
        String serializer = SerializerType.getByCode(serializerCode).getSerializeName();
        int dataSize = in.readInt();
        if(dataSize < 0){
            ctx.close();
        }
        // 若当前可读字节数小于消息长度，则重置readerIndex，直至可以获取到消息长度的字节数
        if(in.readableBytes() < dataSize){
            in.resetReaderIndex();
            return;
        }
        byte[] data = new byte[dataSize];
        // 从channel读取数据至byte数组data
        in.readBytes(data);
        Object obj = SerializerEngine.deserialize(data,genericClass,serializer);
        out.add(obj);
        long duration = System.currentTimeMillis()-startTime;
        LOGGER.info("[{}]协议解码耗时{}ms",serializer,duration);
    }
}
