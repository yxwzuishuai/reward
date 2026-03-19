package com.example.business.controller;

import com.example.business.dto.OrderDetailDTO;
import com.example.business.service.OrderService;
import com.example.common.page.CursorPageRequest;
import com.example.common.page.CursorPageResult;
import com.example.common.page.HybridPageRequest;
import com.example.common.page.HybridPageResult;
import com.example.common.result.R;
import org.springframework.web.bind.annotation.*;

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
     *
     * 跳页:   GET /order/hybrid-page?pageNum=100&pageSize=20
     * 带条件: GET /order/hybrid-page?pageNum=100&pageSize=20&status=1
     * 深翻页: GET /order/hybrid-page?cursor=10020&pageSize=20  (自动走游标)
     */
    @GetMapping("/hybrid-page")
    public R<HybridPageResult<OrderDetailDTO>> hybridPage(HybridPageRequest request,
                                                           @RequestParam(required = false) Integer status) {
        return R.ok(orderService.queryOrderPageHybrid(request, status));
    }
}
