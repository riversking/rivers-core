package com.rivers.core.feign;

import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface DynamicFeign {

    @PostMapping("{url}")
    String executePostApi(@PathVariable("url") String url, @RequestBody Object params);

    @GetMapping("{url}")
    String executeGetApi(@PathVariable("url") String url, @SpringQueryMap Object params);
}
