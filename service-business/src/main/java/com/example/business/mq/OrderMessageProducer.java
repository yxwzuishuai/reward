package com.example.business.mq;

import com.example.business.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 订单消息生产者
 */
@Slf4j
@Component
public class OrderMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public OrderMessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发送订单创建消息
     */
    public void sendOrderCreate(OrderMessage message) {
        log.info("发送订单创建消息: orderNo={}, messageId={}", message.getOrderNo(), message.getMessageId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATE_ROUTING_KEY,
                message
        );
    }

    /**
     * 发送延迟消息（订单超时取消）
     * 利用 per-message TTL + DLX 实现延迟投递
     */
    public void sendOrderDelay(OrderMessage message, long delayMillis) {
        log.info("发送订单延迟消息: orderNo={}, delay={}ms", message.getOrderNo(), delayMillis);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_DELAY_ROUTING_KEY,
                message,
                msg -> {
                    msg.getMessageProperties().setExpiration(String.valueOf(delayMillis));
                    return msg;
                }
        );
    }
}
