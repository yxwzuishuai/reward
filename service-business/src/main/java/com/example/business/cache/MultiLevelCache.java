package com.example.business.cache;

import java.util.function.Function;

/**
 * 多级缓存接口
 * L1: Caffeine 本地缓存
 * L2: Redis 分布式缓存
 */
public interface MultiLevelCache {

    /**
     * 查询缓存，L1 -> L2 -> dbLoader 逐级降级
     *
     * @param key      缓存键
     * @param dbLoader 数据库回源函数
     * @param clazz    返回值类型
     * @return 缓存值，可能为 null
     */
    <V> V get(String key, Function<String, V> dbLoader, Class<V> clazz);

    /**
     * 写入缓存（同时写 L1 和 L2）
     */
    void put(String key, Object value);

    /**
     * 删除缓存（同时删 L1 和 L2）
     */
    void evict(String key);

    /**
     * 批量删除（支持前缀匹配）
     */
    void evictByPrefix(String prefix);
}
