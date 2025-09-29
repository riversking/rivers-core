package com.rivers.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.protobuf.GeneratedMessage;
import com.rivers.core.proto.ProtobufDeserializer;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.protobuf.ProtobufJsonFormatHttpMessageConverter;

@Configuration(proxyBeanMethods = false)
public class ProtobufJacksonConfig {

    @Configuration(proxyBeanMethods = false)
    static class ObjectMapperBeanPostProcessor implements BeanPostProcessor {

        @Override
        public Object postProcessAfterInitialization(@NotNull Object bean, @NotNull String beanName)
                throws BeansException {
            // 1. 检查当前初始化的Bean是否是Jackson的ObjectMapper
            if (bean instanceof ObjectMapper objectMapper) {
                // 2. 对其进行定制化
                // 注册我们的Protobuf模块
                // 【关键】创建并注册 Module
                SimpleModule protobufModule = new SimpleModule("ProtobufModule");

                // 将我们的序列化器注册为处理所有 GeneratedMessageV3 子类的通用序列化器
                // 注意：我们使用 new ProtobufSerializer()，它有一个无参构造函数
                protobufModule.addSerializer(GeneratedMessage.class, new ProtobufSerializer<>());

                // 将反序列化器也注册回来
                protobufModule.addDeserializer(GeneratedMessage.class, new ProtobufDeserializer<>());
                objectMapper.registerModule(protobufModule);
                // 配置特性
                objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
                // 3. 返回修改后的Bean
                return objectMapper;
            }
            // 4. 如果不是ObjectMapper，则原样返回
            return bean;
        }
    }

    @Bean
    public ProtobufJsonFormatHttpMessageConverter protobufJsonFormatHttpMessageConverter() {
        return new ProtobufJsonFormatHttpMessageConverter();
    }

}
