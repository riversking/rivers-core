package com.rivers.core.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * RestClient 配置（Spring 6.1+ 同步 HTTP 客户端），服务间与外部 URL 两种场景区分提供：
 * <ul>
 *     <li>{@code loadBalancedRestClientBuilder}：服务间调用版——@LoadBalanced 限定符与 Boot 默认
 *     {@code RestClient.Builder} 共存互不影响；url 写服务名（如 http://user-server/xxx），
 *     由 Spring Cloud LoadBalancer + Nacos 服务发现解析实例。使用方注入
 *     {@code @LoadBalanced RestClient.Builder} 后 build() 使用（Spring Cloud 官方模式）；</li>
 *     <li>{@code externalRestClient}：外部 URL 直连版——url 写完整地址（http(s)://域名/路径），
 *     不走服务发现；基于 JDK HttpClient（HTTP/2 + 内建连接池），连接超时 10s、读超时 30s。</li>
 * </ul>
 * 使用方式：外部调用按 bean 名注入（{@code RestClient externalRestClient}）；
 * 需要定制（baseUrl、默认头等）时可用 {@code externalRestClient.mutate()} 派生新实例。
 */
@AutoConfiguration
@ConditionalOnClass(RestClient.class)
public class RestClientConfig {

    /**
     * 服务间调用版：注入 {@code @LoadBalanced RestClient.Builder} 后 build() 使用
     */
    @Bean
    @LoadBalanced
    @ConditionalOnMissingBean(name = "loadBalancedRestClientBuilder")
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    /**
     * 外部 URL 直连版：JDK HttpClient 实现，带连接/读超时与重定向跟随
     */
    @Bean
    @ConditionalOnMissingBean(name = "externalRestClient")
    public RestClient externalRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
