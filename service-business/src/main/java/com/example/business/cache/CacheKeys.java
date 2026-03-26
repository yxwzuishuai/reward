package com.example.business.cache;

/**
 * 缓存键常量
 */
public final class CacheKeys {

    private CacheKeys() {}

    public static String orderDetail(String orderNo) {
        return "order:detail:" + orderNo;
    }

    public static String userOrderPage(Long userId, int pageNo) {
        return "order:user:" + userId + ":page:" + pageNo;
    }

    public static String productStock(Long productId) {
        return "product:stock:" + productId;
    }

    public static String userOrderPrefix(Long userId) {
        return "order:user:" + userId;
    }
}
