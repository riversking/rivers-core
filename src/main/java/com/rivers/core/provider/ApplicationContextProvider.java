package com.rivers.core.provider;

import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 静态上下文持有者：由 {@link com.rivers.core.config.RiversCoreAutoConfiguration} 统一装配，
 * 供静态工具（如 SpringContextUtil）在非托管代码中获取 Bean。
 */
public class ApplicationContextProvider implements ApplicationContextAware {

    @Getter
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        ApplicationContextProvider.applicationContext = applicationContext;
    }

}
