package com.rivers.core.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * RestClient 通用调用工具，覆盖两类场景（与 {@link com.rivers.core.config.RestClientConfig} 的客户端一一对应）：
 * <ul>
 *     <li>服务间调用（{@code xxxForService}）：入参为服务名 + 路径（如 "user-server"、"/user/getUser"），
 *     自动拼为 {@code http://服务名/路径}，由 Spring Cloud LoadBalancer + Nacos 服务发现解析实例；</li>
 *     <li>外部 URL 调用（{@code xxxForUrl}）：入参为完整地址（http(s)://域名/路径），直连外部服务，不走服务发现。</li>
 * </ul>
 * 响应体按 JSON 反序列化为目标类型（普通类型用 Class，泛型如 ResultVO&lt;UserDetailRes&gt; 用
 * ParameterizedTypeReference）；HTTP 4xx/5xx 抛 RestClientResponseException 由调用方处理；
 * 需要原始 JSON 字符串时使用不带类型参数的重载。
 * <p>
 * 用法示例：
 * <pre>{@code
 * // 服务间：结果泛型用 ParameterizedTypeReference
 * ResultVO<UserDetailRes> res = restClientUtil.getForService("user-server", "/user/getUser",
 *         new ParameterizedTypeReference<ResultVO<UserDetailRes>>() {});
 * // 外部 URL：原始 JSON
 * String json = restClientUtil.getForUrl("https://api.example.com/data");
 * }</pre>
 * 由 {@link com.rivers.core.config.RiversCoreAutoConfiguration} 统一装配，消费方可自行定义同名 Bean 覆盖。
 */
public class RestClientUtil {

    /**
     * 服务间调用客户端：url 写服务名，由 LoadBalancer 解析实例
     */
    private final RestClient serviceClient;

    /**
     * 外部直连客户端：url 写完整地址
     */
    private final RestClient externalClient;

    public RestClientUtil(RestClient serviceClient, RestClient externalClient) {
        this.serviceClient = serviceClient;
        this.externalClient = externalClient;
    }

    // ==================== 服务间调用（服务名走服务发现） ====================

    public String getForService(String serviceName, String path) {
        return serviceClient.get().uri(serviceUrl(serviceName, path))
                .retrieve().body(String.class);
    }

    public <T> T getForService(String serviceName, String path, Class<T> responseType) {
        return serviceClient.get().uri(serviceUrl(serviceName, path))
                .retrieve().body(responseType);
    }

    public <T> T getForService(String serviceName, String path, ParameterizedTypeReference<T> typeRef) {
        return serviceClient.get().uri(serviceUrl(serviceName, path))
                .retrieve().body(typeRef);
    }

    public String postForService(String serviceName, String path, Object body) {
        return serviceClient.post().uri(serviceUrl(serviceName, path))
                .contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().body(String.class);
    }

    public <T> T postForService(String serviceName, String path, Object body, Class<T> responseType) {
        return serviceClient.post().uri(serviceUrl(serviceName, path))
                .contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().body(responseType);
    }

    public <T> T postForService(String serviceName, String path, Object body, ParameterizedTypeReference<T> typeRef) {
        return serviceClient.post().uri(serviceUrl(serviceName, path))
                .contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().body(typeRef);
    }

    // ==================== 外部 URL 调用（直连） ====================

    public String getForUrl(String url) {
        return externalClient.get().uri(url)
                .retrieve().body(String.class);
    }

    public <T> T getForUrl(String url, Class<T> responseType) {
        return externalClient.get().uri(url)
                .retrieve().body(responseType);
    }

    public <T> T getForUrl(String url, ParameterizedTypeReference<T> typeRef) {
        return externalClient.get().uri(url)
                .retrieve().body(typeRef);
    }

    public String postForUrl(String url, Object body) {
        return externalClient.post().uri(url)
                .contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().body(String.class);
    }

    public <T> T postForUrl(String url, Object body, Class<T> responseType) {
        return externalClient.post().uri(url)
                .contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().body(responseType);
    }

    public <T> T postForUrl(String url, Object body, ParameterizedTypeReference<T> typeRef) {
        return externalClient.post().uri(url)
                .contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().body(typeRef);
    }

    /**
     * 服务名 + 路径拼接为服务发现可解析的 url（http://服务名/路径），路径带不带前导 / 均可
     */
    private String serviceUrl(String serviceName, String path) {
        if (path == null || path.isEmpty()) {
            return "http://" + serviceName;
        }
        return "http://" + serviceName + (path.startsWith("/") ? path : "/" + path);
    }
}
