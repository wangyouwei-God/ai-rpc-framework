package com.aicore.rpc.message;

public class ProtocolConstants {
    public static final int MAGIC_NUMBER = 0xCAFEBABE;
    public static final byte VERSION = 1;

    public static final int HEADER_LENGTH = 15; // 魔数(4)+版本(1)+序列化(1)+类型(1)+ID(4)+长度(4)

    // Message Types
    public static final byte TYPE_REQUEST = 0;
    public static final byte TYPE_RESPONSE = 1;
    public static final byte TYPE_HEARTBEAT_REQUEST = 2;
    public static final byte TYPE_HEARTBEAT_RESPONSE = 3;

    // Serializer Types
    public static final byte SERIALIZER_JDK = 0;
    public static final byte SERIALIZER_PROTOSTUFF = 1;
}
