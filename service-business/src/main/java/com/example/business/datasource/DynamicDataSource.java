package com.example.business.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态数据源路由
 * 基于 ThreadLocal 实现读写分离
 */
public class DynamicDataSource extends AbstractRoutingDataSource {

    private static final ThreadLocal<DataSourceType> CONTEXT = new ThreadLocal<>();

    @Override
    protected Object determineCurrentLookupKey() {
        DataSourceType type = CONTEXT.get();
        return type != null ? type : DataSourceType.MASTER;
    }

    public static void setReadOnly() {
        CONTEXT.set(DataSourceType.SLAVE);
    }

    public static void setReadWrite() {
        CONTEXT.set(DataSourceType.MASTER);
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
