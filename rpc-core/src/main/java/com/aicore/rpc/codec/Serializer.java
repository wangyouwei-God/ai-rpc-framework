package com.aicore.rpc.codec;

public interface Serializer {
    <T> byte[] serialize(T object);
    <T> T deserialize(byte[] bytes, Class<T> clazz);
}
