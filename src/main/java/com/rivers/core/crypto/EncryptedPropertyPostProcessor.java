package com.rivers.core.crypto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * 扫描 Environment 中所有 ENC(...) 前缀的配置值，解密后注入最高优先级属性源，
 * 使 spring.datasource.password 等敏感配置在绑定前即为明文。
 * LOWEST_PRECEDENCE 保证本处理器最后执行，其 addFirst 不会被后续处理器覆盖。
 */
@Slf4j
public class EncryptedPropertyPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> decrypted = new HashMap<>();
        for (PropertySource<?> source : environment.getPropertySources()) {
            if (!(source instanceof EnumerablePropertySource<?> enumerable)) {
                continue;
            }
            for (String name : enumerable.getPropertyNames()) {
                Object value = enumerable.getProperty(name);
                if (value instanceof String text && AesGcmCipher.isEncrypted(text)) {
                    decrypted.put(name, AesGcmCipher.decrypt(text));
                }
            }
        }
        if (decrypted.isEmpty()) {
            return;
        }
        log.info("已解密 {} 个加密配置项并注入明文属性源", decrypted.size());
        environment.getPropertySources().addFirst(new MapPropertySource("decrypted-config", decrypted));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
