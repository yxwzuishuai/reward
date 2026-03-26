package com.example.business.datasource;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 数据源切面：拦截 @ReadOnly 注解，自动切换到从库
 */
@Slf4j
@Aspect
@Order(-1)
@Component
public class DataSourceAspect {

    @Around("@annotation(readOnly)")
    public Object around(ProceedingJoinPoint point, ReadOnly readOnly) throws Throwable {
        try {
            DynamicDataSource.setReadOnly();
            log.debug("切换到从库: {}", point.getSignature().getName());
            return point.proceed();
        } finally {
            DynamicDataSource.clear();
            log.debug("清理数据源上下文");
        }
    }
}
