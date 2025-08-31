package com.rivers.core.page;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class PageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 9010549281693342232L;

    private Integer currentPage;

    private Integer pageSize;
}
