package com.rivers.core.config;

import io.netty.channel.ChannelOption;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

/**
 * 外部 URL WebClient 配置：与 {@link WebClientConfig} 的 @LoadBalanced 服务间客户端区分——
 * <p>
 * WebClientConfig 产出的是服务间调用用的 {@code WebClient.Builder}（url 写服务名、走服务发现）；
 * 本配置产出直连外部 URL（第三方 API 等）的 {@code WebClient} 实例，url 必须写完整地址
 * （http(s)://域名/路径），不走服务发现，并针对外部调用做了以下优化：
 * <ul>
 *     <li>独立的 Reactor Netty 连接池：与内部调用隔离，避免外部慢请求占满共用连接；</li>
 *     <li>连接超时 10s、响应超时 30s：外部服务不可用时快速失败，不长时间挂起；</li>
 *     <li>开启压缩（gzip 协商）与重定向跟随；</li>
 *     <li>响应缓冲上限放宽到 16MB：外部数据接口（如全量列表）响应可能远超默认 256KB，避免 DataBufferLimitException。</li>
 * </ul>
 * 使用方式：按 bean 名注入即可（{@code WebClient externalWebClient}），
 * 需要定制（baseUrl、拦截器等）时可用 {@code externalWebClient.mutate()} 派生新实例。
 */
@AutoConfiguration
@ConditionalOnClass(WebClient.class)
public class ExternalWebClientConfig {

    @Bean
    @ConditionalOnMissingBean(name = "externalWebClient")
    public WebClient externalWebClient() {
        ConnectionProvider provider = ConnectionProvider.builder("riversExternalWebClient")
                .maxConnections(200)
                .pendingAcquireTimeout(Duration.ofSeconds(10))
                .maxIdleTime(Duration.ofSeconds(30))
                .build();
        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofSeconds(30))
                .compress(true)
                .followRedirect(true);
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                        .build())
                .build();
    }
}
