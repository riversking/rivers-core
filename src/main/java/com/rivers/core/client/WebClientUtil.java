package com.rivers.core.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * WebClient 通用调用工具：外部 URL 直连（响应式，基于 externalWebClient，
 * 自带独立连接池、连接超时 10s、响应超时 30s、16MB 大响应缓冲等优化）。
 * <p>
 * url 写完整地址（http(s)://域名/路径），不走服务发现；返回 Mono，
 * 未订阅不会发起请求，调用方可在链上追加超时/重试/降级（如 onErrorResume）；
 * HTTP 4xx/5xx 以 WebClientResponseException 形式进入错误链。
 * <p>
 * 用法示例：
 * <pre>{@code
 * // 原始 JSON
 * Mono<String> json = webClientUtil.get("https://api.example.com/data");
 * // 反序列化为列表（泛型用 ParameterizedTypeReference）
 * Mono<List<Game>> games = webClientUtil.get(url, new ParameterizedTypeReference<List<Game>>() {});
 * }</pre>
 * 由 {@link com.rivers.core.config.RiversCoreAutoConfiguration} 统一装配，消费方可自行定义同名 Bean 覆盖。
 */
public class WebClientUtil {

    private final WebClient externalWebClient;

    public WebClientUtil(WebClient externalWebClient) {
        this.externalWebClient = externalWebClient;
    }

    public Mono<String> get(String url) {
        return externalWebClient.get().uri(url)
                .retrieve().bodyToMono(String.class);
    }

    public <T> Mono<T> get(String url, Class<T> responseType) {
        return externalWebClient.get().uri(url)
                .retrieve().bodyToMono(responseType);
    }

    public <T> Mono<T> get(String url, ParameterizedTypeReference<T> typeRef) {
        return externalWebClient.get().uri(url)
                .retrieve().bodyToMono(typeRef);
    }

    /**
     * POST JSON：body 传对象（按 JSON 序列化），需要原始 JSON 串请自行构造或使用 WebClient 原始链
     */
    public Mono<String> post(String url, Object body) {
        return externalWebClient.post().uri(url)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .retrieve().bodyToMono(String.class);
    }

    public <T> Mono<T> post(String url, Object body, Class<T> responseType) {
        return externalWebClient.post().uri(url)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .retrieve().bodyToMono(responseType);
    }

    public <T> Mono<T> post(String url, Object body, ParameterizedTypeReference<T> typeRef) {
        return externalWebClient.post().uri(url)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .retrieve().bodyToMono(typeRef);
    }
}
