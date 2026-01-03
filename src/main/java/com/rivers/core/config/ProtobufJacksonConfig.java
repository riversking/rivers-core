package com.rivers.core.config;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.util.JsonFormat;
import com.rivers.core.proto.ProtobufDeserializer;
import com.rivers.core.proto.ProtobufSerializer;
import org.springframework.boot.http.codec.CodecCustomizer;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.protobuf.ProtobufJsonDecoder;
import org.springframework.http.codec.protobuf.ProtobufJsonEncoder;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class ProtobufJacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        return builder -> builder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(SerializationFeature.FAIL_ON_SELF_REFERENCES)
                .changeDefaultPropertyInclusion(incl ->
                        incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .addModule(new SimpleModule("ProtobufModule")
                        .addSerializer(GeneratedMessage.class, new ProtobufSerializer<>())
                        .addDeserializer(GeneratedMessage.class, new ProtobufDeserializer<>()))
                .build();
    }


    @Bean
    public CodecCustomizer protobufCodecCustomizer() {
        return configurer -> {
            JsonFormat.Parser parser = JsonFormat.parser().ignoringUnknownFields();
            JsonFormat.Printer printer = JsonFormat.printer();
            // 注册到 customCodecs（优先级高于默认的 Jackson）
            configurer.customCodecs().registerWithDefaultConfig(
                    new ProtobufJsonDecoder(parser)
            );
            configurer.customCodecs().registerWithDefaultConfig(
                    new ProtobufJsonEncoder(printer)
            );
        };
    }
}
