package com.aicore.rpc.codec;

import com.aicore.rpc.message.ProtocolConstants;

public class SerializerFactory {

    // 默认使用 Protostuff
    private static final Serializer protostuffSerializer = new ProtostuffSerializer();

    public static Serializer getSerializer(byte serializerType) {
        switch (serializerType) {
            case ProtocolConstants.SERIALIZER_PROTOSTUFF:
                return protostuffSerializer;
            // case ProtocolConstants.SERIALIZER_JDK:
            //     return new JdkSerializer(); // 如果需要支持JDK
            default:
                throw new IllegalArgumentException("Unsupported serializer type: " + serializerType);
        }
    }
}
