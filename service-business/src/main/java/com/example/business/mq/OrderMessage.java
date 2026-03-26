package com.example.business.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单消息体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage implements Serializable {

    /** 消息唯一ID（用于幂等） */
    private String messageId;
    /** 订单号 */
    private String orderNo;
    /** 用户ID */
    private Long userId;
    /** 商品ID */
    private Long productId;
    /** 店铺ID */
    private Long shopId;
    /** 订单金额 */
    private BigDecimal amount;
    /** 数量 */
    private Integer quantity;
    /** 消息类型：CREATE / CANCEL / TIMEOUT */
    private String messageType;
    /** 创建时间戳 */
    private Long timestamp;
}
