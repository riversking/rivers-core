package com.rivers.core.proto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.Lists;
import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessage;
import com.rivers.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ProtobufSerializer<T extends GeneratedMessage> extends JsonSerializer<T> {

    private static final List<String> usableFieldNames = Lists.newArrayList("total");


    /**
     * 默认构造函数，供 Jackson 框架通过反射实例化使用
     */
    public ProtobufSerializer() {
    }

    @Override
    public void serialize(T message, JsonGenerator gen, SerializerProvider provider) throws IOException {
        var descriptor = message.getDescriptorForType();
        var protoFieldNames = descriptor.getFields().stream()
                .map(Descriptors.FieldDescriptor::getName)
                .collect(Collectors.toSet());
        gen.writeStartObject();
        for (var method : message.getClass().getMethods()) {
            processMethod(message, gen, provider, method, protoFieldNames);
        }
        gen.writeEndObject();
    }

    private void processMethod(T message, JsonGenerator gen, SerializerProvider provider,
                               Method method, Set<String> protoFieldNames) throws IOException {
        var methodName = method.getName();
        var paramCount = method.getParameterCount();

        // 其他方法不做处理
        if (methodName.startsWith("get") && paramCount == 0 && !methodName.equals("getClass")) {
            String fieldName = extractFieldName(methodName);
            if (protoFieldNames.contains(fieldName)) {
                processGetterMethod(message, gen, provider, method, fieldName);
            }
        } else if (methodName.startsWith("has") && paramCount == 0) {
            var fieldName = uncapitalize(methodName.substring(3));
            if (protoFieldNames.contains(fieldName)) {
                processHasMethod(message, gen, provider, method, fieldName);
            }
        }
    }

    private void processGetterMethod(T message, JsonGenerator gen, SerializerProvider provider,
                                     Method method, String fieldName) {
        try {
            ReflectionUtils.makeAccessible(method);
            var value = method.invoke(message);
            if (shouldSerializeValue(value) || usableFieldNames.contains(fieldName)) {
                gen.writeFieldName(fieldName);
                switch (value) {
                    case List<?> list -> serializeList(list, gen, provider);
                    case null -> {
                        // null 值已在 shouldSerializeValue 方法中过滤，此处不需要处理
                    }
                    default -> provider.findValueSerializer(value.getClass()).serialize(value, gen, provider);
                }
            }
        } catch (Exception e) {
            log.error("Failed to serialize field '{}'", fieldName, e);
            throw new BusinessException(fieldName, e);
        }
    }

    private void processHasMethod(T message, JsonGenerator gen, SerializerProvider provider,
                                  Method method, String fieldName) throws IOException {
        try {
            ReflectionUtils.makeAccessible(method);
            if (Boolean.TRUE.equals(method.invoke(message))) {
                var getter = ReflectionUtils.findMethod(message.getClass(),
                        "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1));
                if (getter != null) {
                    ReflectionUtils.makeAccessible(getter);
                    var value = getter.invoke(message);
                    if (shouldSerializeValue(value)) {
                        gen.writeFieldName(fieldName);
                        provider.findValueSerializer(value.getClass()).serialize(value, gen, provider);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to serialize field '{}'", fieldName, e);
            throw new BusinessException(fieldName, e);
        }
    }

    private void serializeList(List<?> list, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartArray();
        try {
            list.stream()
                    .filter(this::shouldSerializeValue)
                    .forEach(item -> {
                        try {
                            provider.findValueSerializer(item.getClass()).serialize(item, gen, provider);
                        } catch (IOException e) {
                            log.error("Failed to serialize list item", e);
                        }
                    });
        } finally {
            gen.writeEndArray();
        }
    }

    private boolean shouldSerializeValue(Object value) {
        return switch (value) {
            case null -> false;
            case String s when s.isEmpty() -> false;
            case Number n when n.doubleValue() == 0.0 -> false;
            case Boolean b when !b -> false;
            default -> true;
        };
    }

    private String extractFieldName(String methodName) {
        String list;
        if (methodName instanceof String s && s.endsWith("List")) {
            list = uncapitalize(s.substring(3, s.length() - 4));
        } else {
            list = uncapitalize(methodName.substring(3));
        }
        return list;
    }

    private String uncapitalize(String str) {
        return switch (str) {
            case "" -> str;
            case null -> null;
            default -> str.substring(0, 1).toLowerCase() + str.substring(1);
        };
    }
}
