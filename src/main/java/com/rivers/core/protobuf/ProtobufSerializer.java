package com.rivers.core.protobuf;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ProtobufSerializer<T extends GeneratedMessage> extends JsonSerializer<T> {

    @Override
    public void serialize(T message, JsonGenerator gen, SerializerProvider provider) throws IOException {
        // 1. 获取消息的Descriptor，这是元数据的来源
        Descriptors.Descriptor descriptor = message.getDescriptorForType();

        // 2. 从Descriptor中提取所有在.proto文件中定义的字段名，并存入一个Set以便快速查找
        Set<String> protoFieldNames = descriptor.getFields().stream()
                .map(Descriptors.FieldDescriptor::getName)
                .collect(Collectors.toSet());

        gen.writeStartObject();

        // 3. 遍历消息中的所有方法
        for (Method method : message.getClass().getMethods()) {
            String methodName = method.getName();

            // 4. 检查方法是否是标准的getter (getXxx, hasXxx)
            if (methodName.startsWith("get") && method.getParameterCount() == 0 && !methodName.equals("getClass")) {
                String fieldName = uncapitalize(methodName.substring(3));

                // 5. 核心判断：如果字段名在.proto定义的列表中，则进行序列化
                if (protoFieldNames.contains(fieldName)) {
                    try {
                        ReflectionUtils.makeAccessible(method);
                        Object value = method.invoke(message);
                        if (!isDefaultValue(value)) {
                            gen.writeFieldName(fieldName);
                            provider.findValueSerializer(value.getClass()).serialize(value, gen, provider);
                        }
                    } catch (Exception e) {
                        log.error("Failed to serialize field '", e);
                    }
                }
            } else if (methodName.startsWith("has") && method.getParameterCount() == 0) {
                // 处理 hasXxx() 方法，通常用于 optional 字段或基本类型
                String fieldName = uncapitalize(methodName.substring(3));
                if (protoFieldNames.contains(fieldName)) {
                    try {
                        ReflectionUtils.makeAccessible(method);
                        // 如果 hasXxx() 返回 true，我们才去获取并序列化 getXxx() 的值
                        if (Boolean.TRUE.equals(method.invoke(message))) {
                            Method getter = ReflectionUtils.findMethod(message.getClass(), "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1));
                            if (getter != null) {
                                ReflectionUtils.makeAccessible(getter);
                                Object value = getter.invoke(message);
                                gen.writeFieldName(fieldName);
                                provider.findValueSerializer(value.getClass()).serialize(value, gen, provider);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Failed to serialize field ", e);
                    }
                }
            }
        }

        gen.writeEndObject();
    }

    private boolean isDefaultValue(Object value) {
        return switch (value) {
            case null -> true;
            case String str when str.isEmpty() -> true;
            case Number number when number.doubleValue() == 0.0 -> true;
            case Boolean b when !b -> true;
            default -> false;
        };
    }

    private String uncapitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }
}
