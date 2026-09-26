package com.rivers.core.client;

import com.rivers.core.exception.BusinessException;
import com.rivers.core.vo.HealthVO;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.management.ServiceNotFoundException;
import java.util.List;

/**
 * 动态服务客户端：按服务名解析实例（含健康校验）后发起 HTTP 调用。
 * 由 {@link com.rivers.core.config.RiversCoreAutoConfiguration} 统一装配。
 */
public class DynamicServiceClient {

    private final WebClient webClient;

    private final DiscoveryClient discoveryClient;

    public DynamicServiceClient(WebClient.Builder builder, DiscoveryClient discoveryClient) {
        this.webClient = builder.build();
        this.discoveryClient = discoveryClient;
    }

    /**
     * 通过服务名获取并校验服务实例，然后执行GET请求
     */
    public <T> Mono<T> get(String serviceName, String path, Class<T> responseType) {
        return getServiceInstance(serviceName)
                .flatMap(instance -> {
                    String url = instance.getUri().toString() + path;
                    return webClient.get()
                            .uri(url)
                            .retrieve()
                            .bodyToMono(responseType);
                });
    }

    /**
     * 通过服务名获取并校验服务实例，然后执行POST请求
     */
    public <T, R> Mono<T> post(String serviceName, String path, R body, Class<T> responseType) {
        return getServiceInstance(serviceName)
                .flatMap(instance -> {
                    String url = "lb://" + instance.getServiceId() + path;
                    return webClient.post()
                            .uri(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(body)
                            .retrieve()
                            .bodyToMono(responseType);
                });
    }

    /**
     * 获取并校验服务实例
     */
    private Mono<ServiceInstance> getServiceInstance(String serviceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        if (instances.isEmpty()) {
            return Mono.error(new ServiceNotFoundException("Service not found: " + serviceName));
        }
        // 校验服务实例健康状态
        return Flux.fromIterable(instances)
                .filterWhen(this::isServiceHealthy)
                .next()
                .switchIfEmpty(Mono.error(new BusinessException("No healthy instances for service: " + serviceName)));
    }

    /**
     * 校验服务实例是否健康
     */
    private Mono<Boolean> isServiceHealthy(ServiceInstance instance) {
        String healthUrl = "lb://" + instance.getServiceId() + "/actuator/health";
        return webClient.get()
                .uri(healthUrl)
                .retrieve()
                .bodyToMono(HealthVO.class)
                .map(health -> "UP".equals(health.getStatus()))
                .onErrorReturn(false);
    }

}
