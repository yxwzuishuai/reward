package com.example.business.service;

import com.example.business.cache.CacheKeys;
import com.example.business.cache.MultiLevelCache;
import com.example.business.datasource.ReadOnly;
import com.example.business.dto.OrderDetailDTO;
import com.example.business.mapper.OrderMapper;
import com.example.business.mq.OrderMessage;
import com.example.business.mq.OrderMessageProducer;
import com.example.common.id.OrderNoGenerator;
import com.example.common.page.CursorPageRequest;
import com.example.common.page.CursorPageResult;
import com.example.common.page.HybridPageRequest;
import com.example.common.page.HybridPageResult;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 订单服务层（集成多级缓存、读写分离、RabbitMQ 异步）
 */
@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final MultiLevelCache cacheManager;
    private final OrderMessageProducer messageProducer;
    private final StringRedisTemplate redisTemplate;

    public OrderService(OrderMapper orderMapper,
                        MultiLevelCache cacheManager,
                        OrderMessageProducer messageProducer,
                        StringRedisTemplate redisTemplate) {
        this.orderMapper = orderMapper;
        this.cacheManager = cacheManager;
        this.messageProducer = messageProducer;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 查询订单详情（走多级缓存 L1 -> L2 -> DB）
     */
    public OrderDetailDTO getOrderDetail(String orderNo) {
        return cacheManager.get(
                CacheKeys.orderDetail(orderNo),
                key -> orderMapper.selectOrderDetailByOrderNo(orderNo),
                OrderDetailDTO.class
        );
    }

    /**
     * 游标分页查询订单列表（只读，路由到从库）
     */
    @ReadOnly
    public CursorPageResult<OrderDetailDTO> queryOrderPage(CursorPageRequest request) {
        List<OrderDetailDTO> records = orderMapper.selectOrderDetailByCursor(
                request.getSafeCursor(), request.getSafePageSize());
        return CursorPageResult.of(records, request.getSafePageSize(), OrderDetailDTO::getOrderId);
    }

    /**
     * 带条件的游标分页查询（只读，路由到从库）
     */
    @ReadOnly
    public CursorPageResult<OrderDetailDTO> queryOrderPageWithCondition(CursorPageRequest request, Integer status) {
        List<OrderDetailDTO> records = orderMapper.selectOrderDetailByCursorWithCondition(
                request.getSafeCursor(), request.getSafePageSize(), status);
        return CursorPageResult.of(records, request.getSafePageSize(), OrderDetailDTO::getOrderId);
    }

    /**
     * 混合分页查询（只读，路由到从库）
     * 前 N 页走 OFFSET，超过阈值自动切换为游标分页
     */
    @ReadOnly
    public HybridPageResult<OrderDetailDTO> queryOrderPageHybrid(HybridPageRequest request, Integer status) {
        int pageSize = request.getSafePageSize();
        if (request.shouldUseCursor()) {
            List<OrderDetailDTO> records = orderMapper.selectOrderDetailByCursorWithCondition(
                    request.getSafeCursor(), pageSize, status);
            return HybridPageResult.ofCursor(records, pageSize, OrderDetailDTO::getOrderId);
        } else {
            long total = orderMapper.countOrders(status);
            List<OrderDetailDTO> records = orderMapper.selectOrderDetailByOffset(
                    request.getOffset(), pageSize, status);
            return HybridPageResult.ofOffset(records, request.getSafePageNum(), pageSize, total, OrderDetailDTO::getOrderId);
        }
    }

    /**
     * 异步创建订单（通过 RabbitMQ 削峰）
     * 快速返回订单号，实际订单由消费者异步创建
     */
    public String asyncCreateOrder(Long userId, Long productId, Long shopId,
                                   BigDecimal amount, Integer quantity) {
        // 预检库存（快速失败）
        String stockStr = redisTemplate.opsForValue().get(CacheKeys.productStock(productId));
        if (stockStr != null && Long.parseLong(stockStr) <= 0) {
            throw new RuntimeException("库存不足");
        }

        // 生成订单号
        String orderNo = OrderNoGenerator.generate(userId);

        // 构建消息
        OrderMessage message = OrderMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .orderNo(orderNo)
                .userId(userId)
                .productId(productId)
                .shopId(shopId)
                .amount(amount)
                .quantity(quantity)
                .messageType("CREATE")
                .timestamp(System.currentTimeMillis())
                .build();

        // 发送订单创建消息
        messageProducer.sendOrderCreate(message);

        // 发送延迟消息（30分钟超时取消）
        messageProducer.sendOrderDelay(message, 30 * 60 * 1000);

        return orderNo;
    }

    /**
     * 订单写入后清除相关缓存
     */
    public void evictOrderCache(String orderNo, Long userId) {
        cacheManager.evict(CacheKeys.orderDetail(orderNo));
        cacheManager.evictByPrefix(CacheKeys.userOrderPrefix(userId));
    }
}
