package com.rivers.core.exception;

import java.io.Serial;

public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BusinessException(Throwable e) {
        super(e);
    }

    public BusinessException(String message, Throwable msg) {
        super(message, msg);
    }


}
