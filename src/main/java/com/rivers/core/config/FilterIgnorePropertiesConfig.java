package com.rivers.core.config;

import com.google.common.collect.Lists;
import lombok.Data;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.List;

/**
 * @author riversking
 */
@AutoConfiguration
@RefreshScope
@ConfigurationProperties(prefix = "ignore")
@Data
public class FilterIgnorePropertiesConfig {

    /**
     * 放行终端配置，网关不校验此处的终端
     */
    private List<String> clients = Lists.newArrayList();
    /**
     * 放行url,放行的url不再被安全框架拦截
     */
    private List<String> urls = Lists.newArrayList();
    /**
     * 不聚合swagger
     */
    private List<String> swaggerProviders = Lists.newArrayList();

}
