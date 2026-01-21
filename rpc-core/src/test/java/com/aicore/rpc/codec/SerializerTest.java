package com.aicore.rpc.codec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.Serializable;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Serializers (Protostuff and JDK).
 */
class SerializerTest {

    // Test data class for Protostuff (no-arg constructor required)
    public static class TestMessage {
        private String name;
        private int value;
        private boolean active;

        public TestMessage() {
        }

        public TestMessage(String name, int value, boolean active) {
            this.name = name;
            this.value = value;
            this.active = active;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            TestMessage that = (TestMessage) o;
            return value == that.value && active == that.active && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value, active);
        }
    }

    // Test data class for JDK (must implement Serializable)
    public static class SerializableMessage implements Serializable {
        private static final long serialVersionUID = 1L;
        private String content;
        private long timestamp;

        public SerializableMessage() {
        }

        public SerializableMessage(String content, long timestamp) {
            this.content = content;
            this.timestamp = timestamp;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            SerializableMessage that = (SerializableMessage) o;
            return timestamp == that.timestamp && Objects.equals(content, that.content);
        }

        @Override
        public int hashCode() {
            return Objects.hash(content, timestamp);
        }
    }

    // ========== Protostuff Serializer Tests ==========

    @Test
    @DisplayName("Protostuff: Should serialize and deserialize correctly")
    void testProtostuffRoundTrip() {
        ProtostuffSerializer serializer = new ProtostuffSerializer();
        TestMessage original = new TestMessage("test", 42, true);

        byte[] bytes = serializer.serialize(original);
        TestMessage deserialized = serializer.deserialize(bytes, TestMessage.class);

        assertEquals(original, deserialized);
    }

    @Test
    @DisplayName("Protostuff: Should handle null fields")
    void testProtostuffNullFields() {
        ProtostuffSerializer serializer = new ProtostuffSerializer();
        TestMessage original = new TestMessage(null, 0, false);

        byte[] bytes = serializer.serialize(original);
        TestMessage deserialized = serializer.deserialize(bytes, TestMessage.class);

        assertEquals(original, deserialized);
    }

    @Test
    @DisplayName("Protostuff: Should produce non-empty bytes")
    void testProtostuffSerializeProducesBytes() {
        ProtostuffSerializer serializer = new ProtostuffSerializer();
        TestMessage message = new TestMessage("hello", 123, true);

        byte[] bytes = serializer.serialize(message);

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    // ========== JDK Serializer Tests ==========

    @Test
    @DisplayName("JDK: Should serialize and deserialize correctly")
    void testJdkRoundTrip() {
        JdkSerializer serializer = new JdkSerializer();
        SerializableMessage original = new SerializableMessage("content", 123456789L);

        byte[] bytes = serializer.serialize(original);
        SerializableMessage deserialized = serializer.deserialize(bytes, SerializableMessage.class);

        assertEquals(original, deserialized);
    }

    @Test
    @DisplayName("JDK: Should handle null fields")
    void testJdkNullFields() {
        JdkSerializer serializer = new JdkSerializer();
        SerializableMessage original = new SerializableMessage(null, 0);

        byte[] bytes = serializer.serialize(original);
        SerializableMessage deserialized = serializer.deserialize(bytes, SerializableMessage.class);

        assertEquals(original, deserialized);
    }

    @Test
    @DisplayName("JDK: Should produce non-empty bytes")
    void testJdkSerializeProducesBytes() {
        JdkSerializer serializer = new JdkSerializer();
        SerializableMessage message = new SerializableMessage("test", 999);

        byte[] bytes = serializer.serialize(message);

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    // ========== Comparison Tests ==========

    @Test
    @DisplayName("Protostuff should be more compact than JDK")
    void testProtostuffMoreCompact() {
        ProtostuffSerializer protostuff = new ProtostuffSerializer();
        JdkSerializer jdk = new JdkSerializer();

        TestMessage protoMsg = new TestMessage("comparison", 999, true);
        SerializableMessage jdkMsg = new SerializableMessage("comparison", 999);

        byte[] protoBytes = protostuff.serialize(protoMsg);
        byte[] jdkBytes = jdk.serialize(jdkMsg);

        // Protostuff is typically more compact
        assertTrue(protoBytes.length < jdkBytes.length,
                "Protostuff (" + protoBytes.length + ") should be smaller than JDK (" + jdkBytes.length + ")");
    }
}
