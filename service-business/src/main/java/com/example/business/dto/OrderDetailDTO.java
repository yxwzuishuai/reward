package com.example.business.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单详情DTO - 多表join后的结果
 */
@Data
public class OrderDetailDTO {

    private Long orderId;
    private String orderNo;
    private BigDecimal amount;
    private Integer status;
    private LocalDateTime createTime;

    // 来自 user 表
    private String userName;
    private String userPhone;

    // 来自 product 表
    private String productName;
    private String productCategory;

    // 来自 shop 表
    private String shopName;
}
