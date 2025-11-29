package com.rivers.core.entity;

import com.google.common.collect.Maps;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
public class JobParamReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 7284949678274488419L;

    private String taskName;

    private Map<String, Object> params = Maps.newHashMap();
}
