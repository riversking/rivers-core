package com.rivers.core.proto;

import com.google.protobuf.GeneratedMessage;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.Deserializers;
import tools.jackson.databind.module.SimpleModule;

/**
 * Protobuf ↔ Jackson 模块（序列化 + 反序列化，全局覆盖所有 proto 消息子类）。
 * <p>
 * 序列化：按父类 {@link GeneratedMessage} 注册即可命中全部子类（序列化器为层级查找）。
 * <p>
 * 反序列化：Jackson 3 的 {@code SimpleModule#addDeserializer} 按<b>精确类型</b>匹配，
 * 注册父类不会命中具体消息子类（退化为普通 bean 反序列化并<b>静默丢失字段</b>）；
 * 因此这里通过 {@link Deserializers#findBeanDeserializer} 钩子按类型族命中，
 * 一次注册即可覆盖全部 proto 消息子类。
 */
public class ProtobufModule extends SimpleModule {

    public ProtobufModule() {
        super("ProtobufModule");
        addSerializer(GeneratedMessage.class, new ProtobufSerializer<>());
    }

    @Override
    public void setupModule(JacksonModule.SetupContext context) {
        super.setupModule(context);
        context.addDeserializers(new ProtobufDeserializers());
    }

    /** 按类型族命中所有 GeneratedMessage 子类（Jackson 3 精确匹配之外的通用扩展点） */
    private static final class ProtobufDeserializers extends Deserializers.Base {

        @Override
        public boolean hasDeserializerFor(DeserializationConfig config, Class<?> rawType) {
            return rawType != GeneratedMessage.class && GeneratedMessage.class.isAssignableFrom(rawType);
        }

        @Override
        public ValueDeserializer<?> findBeanDeserializer(JavaType type, DeserializationConfig config,
                                                         BeanDescription.Supplier beanDesc) {
            Class<?> raw = type.getRawClass();
            if (raw != GeneratedMessage.class && GeneratedMessage.class.isAssignableFrom(raw)) {
                return new ProtobufDeserializer<>(raw.asSubclass(GeneratedMessage.class));
            }
            return null;
        }
    }
}
