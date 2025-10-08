package com.rivers.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rivers.core.exception.BusinessException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class HttpClientUtil {

    /**
     * Java 11+ HttpClient 工具类
     * <p>
     * 提供了 GET, POST 等常用 HTTP 方法的同步和异步调用。
     * 自动处理 JSON 请求体的序列化和响应体的反序列化。
     *
     */

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final static HttpClient DEFAULT_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public HttpClientUtil() {
    }


    /**
     * 发送 GET 请求（同步）
     *
     * @param url     请求 URL
     * @param headers 请求头
     * @return 响应体字符串
     * @throws BusinessException 如果请求失败
     */
    public static String get(String url, Map<String, String> headers) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET();
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(requestBuilder::header);
        }
        return sendRequest(DEFAULT_CLIENT, requestBuilder.build());
    }

    /**
     * 发送 GET 请求（同步），无自定义请求头
     *
     * @param url 请求 URL
     * @return 响应体字符串
     */
    public static String get(String url) {
        return get(url, null);
    }

    /**
     * 发送 GET 请求（异步）
     *
     * @param url     请求 URL
     * @param headers 请求头
     * @return CompletableFuture<String> 包含响应体字符串
     */
    public static CompletableFuture<String> getAsync(String url, Map<String, String> headers) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET();
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(requestBuilder::header);
        }
        return sendRequestAsync(DEFAULT_CLIENT, requestBuilder.build());
    }

    /**
     * 发送 GET 请求（异步），无自定义请求头
     *
     * @param url 请求 URL
     * @return CompletableFuture<String> 包含响应体字符串
     */
    public static CompletableFuture<String> getAsync(String url) {
        return getAsync(url, null);
    }

    /**
     * 发送 POST 请求（同步），请求体为 JSON
     *
     * @param url     请求 URL
     * @param body    请求体对象（将被序列化为 JSON）
     * @param headers 请求头
     * @param <T>     请求体对象类型
     * @return 响应体字符串
     * @throws BusinessException 如果请求失败或序列化失败
     */
    public static <T> String postJson(String url, T body, Map<String, String> headers) {
        String jsonBody;
        try {
            jsonBody = OBJECT_MAPPER.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Failed to serialize request body to JSON", e);
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        if (headers != null && !headers.isEmpty()) {
            headers.forEach(requestBuilder::header);
        }

        return sendRequest(DEFAULT_CLIENT, requestBuilder.build());
    }

    /**
     * 发送 POST 请求（同步），请求体为 JSON，无自定义请求头
     *
     * @param url  请求 URL
     * @param body 请求体对象（将被序列化为 JSON）
     * @param <T>  请求体对象类型
     * @return 响应体字符串
     */
    public static <T> String postJson(String url, T body) {
        return postJson(url, body, null);
    }

    /**
     * 发送 POST 请求（同步），请求体为表单数据
     *
     * @param url      请求 URL
     * @param formData 表单数据
     * @param headers  请求头
     * @return 响应体字符串
     */
    public static String postForm(String url, Map<String, String> formData, Map<String, String> headers) {
        String formBody = formData.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((p1, p2) -> p1 + "&" + p2)
                .orElse("");

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody));

        if (headers != null && !headers.isEmpty()) {
            headers.forEach(requestBuilder::header);
        }

        return sendRequest(DEFAULT_CLIENT, requestBuilder.build());
    }

    /**
     * 发送 POST 请求（同步），请求体为表单数据，无自定义请求头
     *
     * @param url      请求 URL
     * @param formData 表单数据
     * @return 响应体字符串
     */
    public static String postForm(String url, Map<String, String> formData) {
        return postForm(url, formData, null);
    }

    /**
     * 使用自定义的 HttpClient 发送请求（同步）
     *
     * @param client  自定义的 HttpClient
     * @param request HttpRequest 对象
     * @return 响应体字符串
     * @throws BusinessException 如果请求失败
     */
    public static String sendRequest(HttpClient client, HttpRequest request) {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            // 检查响应状态码，如果不是 2xx，则抛出异常
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("HTTP request failed with status code: " + response.statusCode() + ", body: " + response.body());
            }
            return response.body();
        } catch (Exception e) {
            throw new BusinessException("HTTP request failed", e);
        }
    }

    /**
     * 使用默认的 HttpClient 发送请求（异步）
     *
     * @param request HttpRequest 对象
     * @return CompletableFuture<String> 包含响应体字符串
     */
    public static CompletableFuture<String> sendRequestAsync(HttpRequest request) {
        return sendRequestAsync(DEFAULT_CLIENT, request);
    }

    /**
     * 使用自定义的 HttpClient 发送请求（异步）
     *
     * @param client  自定义的 HttpClient
     * @param request HttpRequest 对象
     * @return CompletableFuture<String> 包含响应体字符串
     */
    public static CompletableFuture<String> sendRequestAsync(HttpClient client, HttpRequest request) {
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new BusinessException("HTTP request failed with status code: " + response.statusCode() + ", body: " + response.body());
                    }
                    return response.body();
                })
                .exceptionally(e -> {
                    // 将原始异常包装成我们的自定义异常
                    throw new BusinessException("HTTP request failed", e.getCause() != null ? e.getCause() : e);
                });
    }
}
