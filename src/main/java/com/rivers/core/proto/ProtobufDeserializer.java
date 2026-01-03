package com.rivers.core.proto;


import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessage;
import com.rivers.core.exception.BusinessException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

import java.lang.reflect.Method;
import java.util.Map.Entry;
import java.util.Set;

// 这是一个通用的Protobuf反序列化器
public class ProtobufDeserializer<T extends GeneratedMessage> extends ValueDeserializer<T> {

    private Class<T> targetType;


    public ProtobufDeserializer() {
        // 调用父类 StdDeserializer 的构造器，传入一个占位类型
        // 因为这个“原型”实例本身不用于反序列化，所以传入什么类型并不重要
        // GeneratedMessageV3.class 是一个合理的占位符
        super();
    }


    private ProtobufDeserializer(Class<T> targetType) {
        super();
        this.targetType = targetType;
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext deserializationContext) {
        JsonNode node = p.readValueAsTree();
        // 通过反射调用消息类的 newBuilder() 方法来获取Builder实例
        try {
            Method newBuilderMethod = targetType.getMethod("newBuilder");
            GeneratedMessage.Builder<?> builder = (GeneratedMessage.Builder<?>) newBuilderMethod.invoke(null);

            Descriptors.Descriptor descriptor = builder.getDescriptorForType();

            // 遍历JSON中的所有字段
            Set<Entry<String, JsonNode>> fields = node.properties();
            while (fields.iterator().hasNext()) {
                Entry<String, JsonNode> field = fields.iterator().next();
                String fieldName = field.getKey();
                JsonNode value = field.getValue();

                // 查找Protobuf中对应的字段描述符
                Descriptors.FieldDescriptor fieldDescriptor = descriptor.findFieldByName(fieldName);
                if (fieldDescriptor != null) {
                    // 根据字段类型设置值
                    if (fieldDescriptor.getType() == Descriptors.FieldDescriptor.Type.STRING) {
                        builder.setField(fieldDescriptor, value.stringValue());
                    } else if (fieldDescriptor.getType() == Descriptors.FieldDescriptor.Type.INT32) {
                        builder.setField(fieldDescriptor, value.asInt());
                    } else if (fieldDescriptor.getType() == Descriptors.FieldDescriptor.Type.BOOL) {
                        builder.setField(fieldDescriptor, value.asBoolean());
                    }
                    // 可以根据需要添加更多类型的处理...
                }
            }
            // @SuppressWarnings("unchecked")
            return (T) builder.build();
        } catch (Exception e) {
            throw new BusinessException("Failed to deserialize Protobuf message", e);
        }
    }
}
