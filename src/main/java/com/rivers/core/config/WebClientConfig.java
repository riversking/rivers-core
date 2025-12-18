package com.rivers.core.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    /**
     * 使用 WebClient 创建 HttpServiceProxyFactory
     */
    @Bean
    public HttpServiceProxyFactory httpServiceProxyFactory(WebClient.Builder webClientBuilder) {
        WebClient webClient = webClientBuilder.build();
        return HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient)).build();
    }
}
