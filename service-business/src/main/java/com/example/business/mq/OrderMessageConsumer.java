package com.example.business.mq;

import com.example.business.cache.CacheKeys;
import com.example.business.cache.MultiLevelCache;
import com.example.business.config.RabbitMQConfig;
import com.example.business.entity.Order;
import com.example.business.mapper.OrderMapper;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 订单消息消费者（含幂等处理）
 */
@Slf4j
@Component
public class OrderMessageConsumer {

    private final OrderMapper orderMapper;
    private final StringRedisTemplate redisTemplate;
    private final MultiLevelCache cacheManager;

    public OrderMessageConsumer(OrderMapper orderMapper,
                                 StringRedisTemplate redisTemplate,
                                 MultiLevelCache cacheManager) {
        this.orderMapper = orderMapper;
        this.redisTemplate = redisTemplate;
        this.cacheManager = cacheManager;
    }

    /**
     * 消费订单创建消息（幂等）
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATE_QUEUE)
    public void onOrderCreate(OrderMessage message, Channel channel,
                               @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        String deduplicateKey = "mq:dedup:" + message.getMessageId();
        try {
            // 幂等检查
            Boolean isNew = redisTemplate.opsForValue()
                    .setIfAbsent(deduplicateKey, "1", 24, TimeUnit.HOURS);
            if (Boolean.FALSE.equals(isNew)) {
                log.info("重复消息，跳过: {}", message.getMessageId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 创建订单
            Order order = buildOrder(message);
            orderMapper.insert(order);

            // 扣减库存（Redis 原子操作）
            String stockKey = CacheKeys.productStock(message.getProductId());
            Long stock = redisTemplate.opsForValue().decrement(stockKey);
            if (stock != null && stock < 0) {
                redisTemplate.opsForValue().increment(stockKey);
                throw new RuntimeException("库存不足");
            }

            // 清除相关缓存
            cacheManager.evictByPrefix(CacheKeys.userOrderPrefix(message.getUserId()));

            log.info("订单创建成功: orderNo={}", message.getOrderNo());
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("订单创建失败: orderNo={}, error={}", message.getOrderNo(), e.getMessage());
            // 删除去重标记，允许重试
            redisTemplate.delete(deduplicateKey);
            // requeue=true 重新入队
            channel.basicNack(deliveryTag, false, true);
        }
    }

    /**
     * 消费订单超时取消消息
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_TIMEOUT_QUEUE)
    public void onOrderTimeout(OrderMessage message, Channel channel,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("订单超时取消: orderNo={}", message.getOrderNo());
            // 更新订单状态为已取消（status = -1）
            orderMapper.updateStatusByOrderNo(message.getOrderNo(), -1);
            // 回滚库存
            String stockKey = CacheKeys.productStock(message.getProductId());
            redisTemplate.opsForValue().increment(stockKey, message.getQuantity());
            // 清除缓存
            cacheManager.evict(CacheKeys.orderDetail(message.getOrderNo()));
            cacheManager.evictByPrefix(CacheKeys.userOrderPrefix(message.getUserId()));

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("订单超时取消失败: orderNo={}", message.getOrderNo(), e);
            channel.basicNack(deliveryTag, false, true);
        }
    }

    private Order buildOrder(OrderMessage message) {
        Order order = new Order();
        order.setOrderNo(message.getOrderNo());
        order.setUserId(message.getUserId());
        order.setProductId(message.getProductId());
        order.setShopId(message.getShopId());
        order.setAmount(message.getAmount());
        order.setQuantity(message.getQuantity());
        order.setStatus(0); // 待支付
        order.setCreateTime(LocalDateTime.now());
        return order;
    }
}
