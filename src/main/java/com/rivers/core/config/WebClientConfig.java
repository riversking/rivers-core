package com.rivers.core.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Mono;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(fallbackHandler()); // 添加统一降级处理
    }

    // 全局降级处理过滤器
    private ExchangeFilterFunction fallbackHandler() {
        return ExchangeFilterFunction.ofResponseProcessor(c -> {
            if (c.statusCode().isError()) {
                return Mono.just(c)
                        .flatMap(response -> {
                            if (response.statusCode().is4xxClientError()) {
                                return Mono.just(ClientResponse.create(HttpStatus.BAD_REQUEST)
                                        .header("Content-Type", "application/json")
                                        .body("{\"code\":\"CLIENT_ERROR\",\"message\":\"客户端错误\"}")
                                        .build());
                            } else {
                                return Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE)
                                        .header("Content-Type", "application/json")
                                        .body("{\"code\":\"SERVER_ERROR\",\"message\":\"服务暂时不可用\"}")
                                        .build());
                            }
                        });
            }
            return Mono.just(c);
        });
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
