package com.rivers.core.feign;

import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;

/**
 * 动态 Feign 客户端工厂：由 {@link com.rivers.core.config.RiversCoreAutoConfiguration} 统一装配。
 */
public class DynamicFeignClientFactory<T> {

    private final FeignClientBuilder feignClientBuilder;

    public DynamicFeignClientFactory(ApplicationContext appContext) {
        this.feignClientBuilder = new FeignClientBuilder(appContext);
    }

    public T getFeignClient(final Class<T> type, String serviceId) {
        return this.feignClientBuilder.forType(type, serviceId).build();
    }
}
