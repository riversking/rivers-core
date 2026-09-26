package com.rivers.core.config;

import com.rivers.core.client.DynamicServiceClient;
import com.rivers.core.feign.DynamicClient;
import com.rivers.core.feign.DynamicFeign;
import com.rivers.core.feign.DynamicFeignClientFactory;
import com.rivers.core.provider.ApplicationContextProvider;
import com.rivers.core.task.BatchController;
import com.rivers.core.util.SpringContextUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * rivers-core 统一自动装配入口：集中托管非配置类（工具、动态客户端、控制器等）的 Bean 注册，
 * 使 AutoConfiguration.imports 仅保留配置类。
 */
@AutoConfiguration
public class RiversCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ApplicationContextProvider applicationContextProvider() {
        return new ApplicationContextProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringContextUtil springContextUtil() {
        return new SpringContextUtil();
    }

    /**
     * WebClient 版动态服务客户端：按服务名发现实例并做健康校验后发起 HTTP 调用。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({WebClient.class, DiscoveryClient.class})
    static class DynamicServiceClientConfiguration {

        @Bean
        @ConditionalOnMissingBean
        DynamicServiceClient dynamicServiceClient(WebClient.Builder webClientBuilder, DiscoveryClient discoveryClient) {
            return new DynamicServiceClient(webClientBuilder, discoveryClient);
        }
    }

    /**
     * Feign 版动态客户端：按服务名动态构建并执行调用。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(FeignClientBuilder.class)
    static class DynamicFeignConfiguration {

        @Bean
        @ConditionalOnMissingBean
        DynamicFeignClientFactory<DynamicFeign> dynamicFeignClientFactory(ApplicationContext applicationContext) {
            return new DynamicFeignClientFactory<>(applicationContext);
        }

        @Bean
        @ConditionalOnMissingBean
        DynamicClient dynamicClient(DynamicFeignClientFactory<DynamicFeign> dynamicFeignClientFactory) {
            return new DynamicClient(dynamicFeignClientFactory);
        }
    }

    /**
     * 批任务执行端点：接收调度端（timer-batch）派发的任务，异步执行本服务注册的 BatchTaskHandler。
     * 非任务执行节点的服务可通过 {@code rivers.batch.executor.enabled=false} 关闭本端点。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestController.class)
    @ConditionalOnProperty(name = "rivers.batch.executor.enabled", havingValue = "true", matchIfMissing = true)
    static class BatchExecutorConfiguration {

        @Bean
        @ConditionalOnMissingBean
        BatchController batchController() {
            return new BatchController();
        }
    }
}
