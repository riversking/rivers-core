package com.rivers.core.exception;

import java.io.Serial;

public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BusinessException(Throwable msg) {
        super(msg);
    }



}
