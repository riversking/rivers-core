package com.rivers.core.proto;


import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.util.JsonFormat;
import com.rivers.core.exception.BusinessException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

import java.lang.reflect.Method;

// 这是一个通用的Protobuf反序列化器
public class ProtobufDeserializer<T extends GeneratedMessage> extends ValueDeserializer<T> {

    private Class<T> targetType;


    public ProtobufDeserializer() {
        // 无参原型：目标类型由 Jackson 上下文解析（createContextual / resolveTargetType）
    }


    public ProtobufDeserializer(Class<T> targetType) {
        this.targetType = targetType;
    }

    /**
     * 上下文感知：以属性类型（或直接解码时的目标类型）实例化带类型的反序列化器。
     * 全局注册 {@code new ProtobufDeserializer<>()} 时由 Jackson 在解析前回调本方法。
     */
    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        if (targetType != null) {
            return this;
        }
        JavaType type = property != null ? property.getType() : ctxt.getContextualType();
        if (type != null && GeneratedMessage.class.isAssignableFrom(type.getRawClass())) {
            return new ProtobufDeserializer<>(type.getRawClass().asSubclass(GeneratedMessage.class));
        }
        return this;
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext deserializationContext) {
        JsonNode node = p.readValueAsTree();
        Class<? extends GeneratedMessage> type = resolveTargetType(deserializationContext);
        // 通过反射调用消息类的 newBuilder() 方法来获取Builder实例
        try {
            Method newBuilderMethod = type.getMethod("newBuilder");
            GeneratedMessage.Builder<?> builder = (GeneratedMessage.Builder<?>) newBuilderMethod.invoke(null);

            // 使用官方 JsonFormat 解析 JSON，支持全部字段类型（嵌套/枚举/repeated/int64 等）
            JsonFormat.parser().ignoringUnknownFields().merge(node.toString(), builder);

            // @SuppressWarnings("unchecked")
            return (T) builder.build();
        } catch (Exception e) {
            throw new BusinessException("Failed to deserialize Protobuf message: " + type.getSimpleName(), e);
        }
    }

    /**
     * 解析目标 proto 类型：优先显式构造类型，兜底 Jackson 上下文类型（直接解码顶层消息时）。
     */
    private Class<? extends GeneratedMessage> resolveTargetType(DeserializationContext ctxt) {
        if (targetType != null) {
            return targetType;
        }
        JavaType type = ctxt.getContextualType();
        if (type != null && GeneratedMessage.class.isAssignableFrom(type.getRawClass())) {
            return type.getRawClass().asSubclass(GeneratedMessage.class);
        }
        throw new BusinessException("Failed to resolve Protobuf target type");
    }
}
