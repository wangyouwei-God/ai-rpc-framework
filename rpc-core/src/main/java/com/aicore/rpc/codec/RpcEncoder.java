package com.aicore.rpc.codec;

import com.aicore.rpc.message.ProtocolConstants;
import com.aicore.rpc.message.RpcMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class RpcEncoder extends MessageToByteEncoder<RpcMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage msg, ByteBuf out) throws Exception {
        // 1. 写入魔数
        out.writeInt(ProtocolConstants.MAGIC_NUMBER);
        // 2. 写入版本号
        out.writeByte(ProtocolConstants.VERSION);
        // 3. 写入序列化算法类型
        out.writeByte(msg.getSerializerType());
        // 4. 写入消息类型
        out.writeByte(msg.getMessageType());
        // 5. 写入消息ID
        out.writeInt(msg.getMessageId());

        // 获取序列化器
        Serializer serializer = SerializerFactory.getSerializer(msg.getSerializerType());
        byte[] data = serializer.serialize(msg.getData());

        // 6. 写入数据长度
        out.writeInt(data.length);
        // 7. 写入数据体
        out.writeBytes(data);
    }
}
