package com.example.business.service;

import com.example.business.dto.OrderDetailDTO;
import com.example.business.mapper.OrderMapper;
import com.example.common.page.CursorPageRequest;
import com.example.common.page.CursorPageResult;
import com.example.common.page.HybridPageRequest;
import com.example.common.page.HybridPageResult;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单服务层
 */
@Service
public class OrderService {

    private final OrderMapper orderMapper;

    public OrderService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    /**
     * 游标分页查询订单详情
     */
    public CursorPageResult<OrderDetailDTO> queryOrderPage(CursorPageRequest request) {
        long cursor = request.getSafeCursor();
        int pageSize = request.getSafePageSize();

        List<OrderDetailDTO> records = orderMapper.selectOrderDetailByCursor(cursor, pageSize);

        return CursorPageResult.of(records, pageSize, OrderDetailDTO::getOrderId);
    }

    /**
     * 带条件的游标分页查询
     */
    public CursorPageResult<OrderDetailDTO> queryOrderPageWithCondition(
            CursorPageRequest request, Integer status) {
        long cursor = request.getSafeCursor();
        int pageSize = request.getSafePageSize();

        List<OrderDetailDTO> records = orderMapper.selectOrderDetailByCursorWithCondition(
                cursor, pageSize, status);

        return CursorPageResult.of(records, pageSize, OrderDetailDTO::getOrderId);
    }


    /**
     * 混合分页查询：前500页用OFFSET（支持跳页），超过后自动切游标（保证性能）
     */
    public HybridPageResult<OrderDetailDTO> queryOrderPageHybrid(HybridPageRequest request, Integer status) {
        int pageSize = request.getSafePageSize();

        if (request.shouldUseCursor()) {
            // 游标模式：页码太深或前端主动传了cursor
            long cursor = request.getSafeCursor();
            List<OrderDetailDTO> records = (status != null)
                    ? orderMapper.selectOrderDetailByCursorWithCondition(cursor, pageSize, status)
                    : orderMapper.selectOrderDetailByCursor(cursor, pageSize);
            return HybridPageResult.ofCursor(records, pageSize, OrderDetailDTO::getOrderId);
        } else {
            // OFFSET模式：支持跳页
            int offset = request.getOffset();
            long total = orderMapper.countOrders(status);
            List<OrderDetailDTO> records = orderMapper.selectOrderDetailByOffset(offset, pageSize, status);
            return HybridPageResult.ofOffset(records, request.getSafePageNum(), pageSize, total, OrderDetailDTO::getOrderId);
        }
    }

}
