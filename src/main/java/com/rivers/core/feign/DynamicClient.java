package com.rivers.core.feign;

/**
 * 动态 Feign 客户端：按服务名动态构建并执行调用。
 * 由 {@link com.rivers.core.config.RiversCoreAutoConfiguration} 统一装配。
 */
public class DynamicClient {

    private final DynamicFeignClientFactory<DynamicFeign> dynamicFeignClientFactory;


    public DynamicClient(DynamicFeignClientFactory<DynamicFeign> dynamicFeignClientFactory) {
        this.dynamicFeignClientFactory = dynamicFeignClientFactory;
    }

    public String executePostApi(String feignName, String url, Object params) {
        DynamicFeign dynamicFeign = dynamicFeignClientFactory.getFeignClient(DynamicFeign.class, feignName);
        return dynamicFeign.executePostApi(url, params);
    }

    public String executeGetApi(String feignName, String url, Object params) {
        DynamicFeign dynamicFeign = dynamicFeignClientFactory.getFeignClient(DynamicFeign.class, feignName);
        return dynamicFeign.executeGetApi(url, params);
    }
}
