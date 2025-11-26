package com.rivers.core.util;

import com.rivers.core.provider.ApplicationContextProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SpringContextUtil {

    public static  <T> T getBean(String bean,Class<T> clazz) {
        ApplicationContext applicationContext = ApplicationContextProvider.getApplicationContext();
        return applicationContext.getBean(bean, clazz);
    }

    public static <T> T getBeanByName(String beanName, Class<T> targetType) {
        ApplicationContext context = ApplicationContextProvider.getApplicationContext();
        return context.getBean(beanName, targetType);
    }

    /**
     * 安全的类型转换方法
     */
    public static <T> T getBeanAsType(String beanName, Class<T> targetType) {
        ApplicationContext context = ApplicationContextProvider.getApplicationContext();
        Object bean = context.getBean(beanName);

        if (targetType.isInstance(bean)) {
            return targetType.cast(bean);
        } else {
            throw new IllegalArgumentException(
                    "Bean " + beanName + " is not instance of " + targetType.getName());
        }
    }

    /**
     * 检查bean是否存在并获取
     */
    public <T> Optional<T> getOptionalBean(String beanName, Class<T> targetType) {
        ApplicationContext context = ApplicationContextProvider.getApplicationContext();

        if (context.containsBean(beanName)) {
            return Optional.of(context.getBean(beanName, targetType));
        }
        return Optional.empty();
    }

    /**
     * 获取bean的实际类型
     */
    public Class<?> getBeanType(String beanName) {
        ApplicationContext context = ApplicationContextProvider.getApplicationContext();
        return context.getType(beanName);
    }
}
