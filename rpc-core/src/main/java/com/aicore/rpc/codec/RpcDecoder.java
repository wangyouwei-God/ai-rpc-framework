package com.aicore.rpc.codec;

import com.aicore.rpc.message.ProtocolConstants;
import com.aicore.rpc.message.RpcMessage;
import com.aicore.rpc.model.RpcRequest;
import com.aicore.rpc.model.RpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class RpcDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 1. 检查可读字节数是否大于协议头长度
        if (in.readableBytes() < ProtocolConstants.HEADER_LENGTH) {
            return; // 数据不够，等待下一次读取
        }

        // 标记当前 readerIndex，以便后续复位
        in.markReaderIndex();

        // 2. 检查魔数
        if (in.readInt() != ProtocolConstants.MAGIC_NUMBER) {
            // 魔数不匹配，可能是攻击或无效数据，关闭连接
            ctx.close();
            return;
        }

        // 3. 读取协议头中的其他字段
        byte version = in.readByte(); // 版本号，可以用于校验
        byte serializerType = in.readByte();
        byte messageType = in.readByte();
        int messageId = in.readInt();
        int dataLength = in.readInt();

        // 4. 检查消息体是否完整
        if (in.readableBytes() < dataLength) {
            // 数据不完整，复位 readerIndex，等待下一次数据到来
            in.resetReaderIndex();
            return;
        }

        // 5. 读取消息体数据
        byte[] data = new byte[dataLength];
        in.readBytes(data);

        // 6. 反序列化消息体
        Serializer serializer = SerializerFactory.getSerializer(serializerType);
        RpcMessage rpcMessage = RpcMessage.builder()
                .serializerType(serializerType)
                .messageType(messageType)
                .messageId(messageId)
                .build();

        // 根据消息类型，反序列化成不同的对象
        if (messageType == ProtocolConstants.TYPE_REQUEST || messageType == ProtocolConstants.TYPE_HEARTBEAT_REQUEST) {
            RpcRequest request = serializer.deserialize(data, RpcRequest.class);
            rpcMessage.setData(request);
        } else if (messageType == ProtocolConstants.TYPE_RESPONSE || messageType == ProtocolConstants.TYPE_HEARTBEAT_RESPONSE) {
            RpcResponse response = serializer.deserialize(data, RpcResponse.class);
            rpcMessage.setData(response);
        }

        // 7. 将解码后的 RpcMessage 对象传递给下一个 Handler
        out.add(rpcMessage);
    }
}