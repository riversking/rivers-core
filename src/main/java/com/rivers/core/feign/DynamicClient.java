package com.rivers.core.feign;

import org.springframework.stereotype.Component;

@Component
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
