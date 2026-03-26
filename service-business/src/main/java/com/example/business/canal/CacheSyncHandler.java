package com.example.business.canal;

import com.example.business.cache.CacheKeys;
import com.example.business.cache.MultiLevelCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 缓存同步处理器
 * 监听 Canal 变更事件，自动失效对应的多级缓存（Caffeine + Redis）
 */
@Slf4j
@Component
public class CacheSyncHandler {

    private final MultiLevelCache cacheManager;

    public CacheSyncHandler(MultiLevelCache cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * 处理表变更事件，根据表名路由到对应的缓存失效逻辑
     */
    public void handle(TableChangeEvent event) {
        switch (event.getTable()) {
            case "orders" -> handleOrderChange(event);
            case "product_stock", "products" -> handleStockChange(event);
            default -> log.debug("忽略表 {} 的变更事件", event.getTable());
        }
    }

    private void handleOrderChange(TableChangeEvent event) {
        Map<String, String> data = event.getData();
        String orderNo = data.get("order_no");
        String userId = data.get("user_id");

        if (orderNo != null) {
            cacheManager.evict(CacheKeys.orderDetail(orderNo));
            log.info("Canal 缓存同步: 失效订单缓存 orderNo={}", orderNo);
        }
        if (userId != null) {
            cacheManager.evictByPrefix(CacheKeys.userOrderPrefix(Long.parseLong(userId)));
            log.info("Canal 缓存同步: 失效用户订单列表缓存 userId={}", userId);
        }
    }

    private void handleStockChange(TableChangeEvent event) {
        Map<String, String> data = event.getData();
        String productId = data.get("product_id");
        if (productId == null) {
            productId = data.get("id");
        }
        if (productId != null) {
            cacheManager.evict(CacheKeys.productStock(Long.parseLong(productId)));
            log.info("Canal 缓存同步: 失效库存缓存 productId={}", productId);
        }
    }
}
