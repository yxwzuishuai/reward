package com.example.business.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.business.dto.OrderDetailDTO;
import com.example.business.service.OrderService;
import com.example.common.page.CursorPageRequest;
import com.example.common.page.CursorPageResult;
import com.example.common.page.HybridPageRequest;
import com.example.common.page.HybridPageResult;
import com.example.common.result.R;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 订单接口
 * <p>
 * 使用示例：
 * 第一页: GET /order/page?pageSize=20
 * 下一页: GET /order/page?cursor=10020&pageSize=20  (cursor为上一页返回的nextCursor)
 * 带条件: GET /order/page?cursor=10020&pageSize=20&status=1
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 游标分页查询订单
     */
    @SentinelResource(value = "order-query", blockHandler = "queryBlockHandler")
    @GetMapping("/page")
    public R<CursorPageResult<OrderDetailDTO>> page(CursorPageRequest request,
                                                     @RequestParam(required = false) Integer status) {
        CursorPageResult<OrderDetailDTO> result;
        if (status != null) {
            result = orderService.queryOrderPageWithCondition(request, status);
        } else {
            result = orderService.queryOrderPage(request);
        }
        return R.ok(result);
    }

    /**
     * 混合分页查询订单
     * 前500页支持跳页（OFFSET），超过后自动切换游标分页
     */
    @SentinelResource(value = "order-query", blockHandler = "hybridQueryBlockHandler")
    @GetMapping("/hybrid-page")
    public R<HybridPageResult<OrderDetailDTO>> hybridPage(HybridPageRequest request,
                                                           @RequestParam(required = false) Integer status) {
        return R.ok(orderService.queryOrderPageHybrid(request, status));
    }

    /**
     * 查询订单详情
     */
    @SentinelResource(value = "order-query", blockHandler = "detailBlockHandler")
    @GetMapping("/detail/{orderNo}")
    public R<OrderDetailDTO> detail(@PathVariable String orderNo) {
        return R.ok(orderService.getOrderDetail(orderNo));
    }

    /**
     * 异步创建订单（通过 RabbitMQ 削峰，快速返回订单号）
     */
    @SentinelResource(value = "order-create", blockHandler = "createBlockHandler", fallback = "createFallback")
    @PostMapping("/create")
    public R<String> create(@RequestBody OrderCreateRequest request) {
        String orderNo = orderService.asyncCreateOrder(
                request.getUserId(),
                request.getProductId(),
                request.getShopId(),
                request.getAmount(),
                request.getQuantity()
        );
        return R.ok(orderNo);
    }

    // ==================== BlockHandler & Fallback ====================

    public R<CursorPageResult<OrderDetailDTO>> queryBlockHandler(CursorPageRequest request,
                                                                  Integer status,
                                                                  BlockException ex) {
        return R.fail("系统繁忙，请稍后重试");
    }

    public R<HybridPageResult<OrderDetailDTO>> hybridQueryBlockHandler(HybridPageRequest request,
                                                                        Integer status,
                                                                        BlockException ex) {
        return R.fail("系统繁忙，请稍后重试");
    }

    public R<OrderDetailDTO> detailBlockHandler(String orderNo, BlockException ex) {
        return R.fail("系统繁忙，请稍后重试");
    }

    public R<String> createBlockHandler(OrderCreateRequest request, BlockException ex) {
        return R.fail("系统繁忙，请稍后重试");
    }

    public R<String> createFallback(OrderCreateRequest request, Throwable ex) {
        return R.fail("订单创建失败，请稍后重试");
    }

    // ==================== 请求体 ====================

    @Data
    public static class OrderCreateRequest {
        private Long userId;
        private Long productId;
        private Long shopId;
        private BigDecimal amount;
        private Integer quantity;
    }
}
