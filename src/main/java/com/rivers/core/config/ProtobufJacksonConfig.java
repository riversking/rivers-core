package com.rivers.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.protobuf.GeneratedMessage;
import com.rivers.core.protobuf.ProtobufDeserializer;
import com.rivers.core.protobuf.ProtobufSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.protobuf.ProtobufJsonFormatHttpMessageConverter;

@Configuration
public class ProtobufJacksonConfig {

    public static final String PROTOBUF_USER_FIELDS_FILTER_ID = "protobufUserFieldsFilter";


    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder.createXmlMapper(false).build();
        SimpleModule protobufModule = new SimpleModule("ProtobufModule");
        protobufModule.addSerializer(GeneratedMessage.class, new ProtobufSerializer<>());
        protobufModule.addDeserializer(GeneratedMessage.class, new ProtobufDeserializer());
        mapper.registerModule(protobufModule);
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        return mapper;
    }

    @Bean
    public ProtobufJsonFormatHttpMessageConverter protobufJsonFormatHttpMessageConverter() {
        return new ProtobufJsonFormatHttpMessageConverter();
    }
}
