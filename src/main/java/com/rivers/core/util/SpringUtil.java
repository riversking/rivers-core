package com.rivers.core.util;

import com.rivers.core.provider.ApplicationContextProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class SpringUtil {

    public <T> T getBean(String bean,Class<T> clazz) {
        ApplicationContext applicationContext = ApplicationContextProvider.getApplicationContext();
        return applicationContext.getBean(bean, clazz);
    }

}
