package com.example.business.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 库存服务 - 基于 Redis + Lua 脚本实现原子操作
 */
@Slf4j
@Service
public class StockService {

    private final StringRedisTemplate redisTemplate;
    private final Executor queryExecutor;

    public StockService(StringRedisTemplate redisTemplate,
                        @Qualifier("queryExecutor") Executor queryExecutor) {
        this.redisTemplate = redisTemplate;
        this.queryExecutor = queryExecutor;
    }

    private static final String STOCK_KEY_PREFIX = "stock:product:";

    private DefaultRedisScript<Long> deductScript;
    private DefaultRedisScript<Long> initScript;
    private DefaultRedisScript<Long> restoreScript;

    @PostConstruct
    public void init() {
        // 预加载 Lua 脚本，避免每次执行都解析
        deductScript = new DefaultRedisScript<>();
        deductScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/stock_deduct.lua")));
        deductScript.setResultType(Long.class);

        initScript = new DefaultRedisScript<>();
        initScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/stock_init.lua")));
        initScript.setResultType(Long.class);

        restoreScript = new DefaultRedisScript<>();
        restoreScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/stock_restore.lua")));
        restoreScript.setResultType(Long.class);
    }

    /**
     * 初始化库存（仅当 Redis 中不存在时设置）
     *
     * @param productId 商品ID
     * @param stock     初始库存
     * @param expireSec 过期时间(秒)，0表示不过期
     * @return true=设置成功, false=已存在
     */
    public boolean initStock(Long productId, int stock, long expireSec) {
        String key = STOCK_KEY_PREFIX + productId;
        Long result = redisTemplate.execute(initScript,
                List.of(key),
                String.valueOf(stock), String.valueOf(expireSec));
        boolean success = result != null && result == 1;
        log.info("初始化库存: productId={}, stock={}, success={}", productId, stock, success);
        return success;
    }

    /**
     * 扣减库存
     *
     * @param productId 商品ID
     * @param quantity  扣减数量
     * @return 剩余库存，-1=key不存在，-2=库存不足
     */
    public long deductStock(Long productId, int quantity) {
        String key = STOCK_KEY_PREFIX + productId;
        Long result = redisTemplate.execute(deductScript,
                List.of(key),
                String.valueOf(quantity));

        if (result == null) {
            log.error("扣减库存异常: productId={}, quantity={}", productId, quantity);
            throw new RuntimeException("库存扣减异常");
        }

        if (result == -1) {
            log.warn("库存key不存在: productId={}", productId);
            throw new RuntimeException("商品库存未初始化");
        }

        if (result == -2) {
            log.warn("库存不足: productId={}, 请求数量={}", productId, quantity);
            throw new RuntimeException("库存不足");
        }

        log.info("扣减库存成功: productId={}, quantity={}, 剩余={}", productId, quantity, result);
        return result;
    }

    /**
     * 恢复库存（订单取消/支付超时回滚）
     *
     * @param productId 商品ID
     * @param quantity  恢复数量
     * @return 恢复后的库存数量
     */
    public long restoreStock(Long productId, int quantity) {
        String key = STOCK_KEY_PREFIX + productId;
        Long result = redisTemplate.execute(restoreScript,
                List.of(key),
                String.valueOf(quantity));

        if (result == null || result == -1) {
            log.error("恢复库存失败: productId={}, quantity={}", productId, quantity);
            throw new RuntimeException("库存恢复失败，key不存在");
        }

        log.info("恢复库存成功: productId={}, quantity={}, 当前库存={}", productId, quantity, result);
        return result;
    }

    /**
     * 查询当前库存
     */
    public long getStock(Long productId) {
        String key = STOCK_KEY_PREFIX + productId;
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? -1 : Long.parseLong(value);
    }

    /**
     * 异步查询单个商品库存
     */
    public CompletableFuture<Long> getStockAsync(Long productId) {
        return CompletableFuture.supplyAsync(() -> getStock(productId), queryExecutor);
    }

    /**
     * 批量异步查询多个商品库存，并行执行
     *
     * @param productIds 商品ID列表
     * @return Map<商品ID, 库存数量>
     */
    public CompletableFuture<Map<Long, Long>> batchGetStockAsync(List<Long> productIds) {
        List<CompletableFuture<Map.Entry<Long, Long>>> futures = productIds.stream()
                .map(id -> CompletableFuture.supplyAsync(
                        () -> Map.entry(id, getStock(id)), queryExecutor))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    Map<Long, Long> result = new HashMap<>();
                    for (CompletableFuture<Map.Entry<Long, Long>> f : futures) {
                        Map.Entry<Long, Long> entry = f.join();
                        result.put(entry.getKey(), entry.getValue());
                    }
                    return result;
                });
    }
}
