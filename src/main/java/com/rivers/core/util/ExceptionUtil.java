package com.rivers.core.util;

import com.rivers.core.exception.BusinessException;
import org.springframework.util.Assert;

public class ExceptionUtil extends Assert {


    public static void throwBusinessException(Throwable msg) {
        throw new BusinessException(msg);
    }


}
