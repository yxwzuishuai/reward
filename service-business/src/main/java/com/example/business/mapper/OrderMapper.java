package com.example.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.business.dto.OrderDetailDTO;
import com.example.business.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单数据访问层
 * <p>
 * 继承 BaseMapper 获得单表 CRUD 能力，
 * 复杂的多表 join + 游标/OFFSET 分页走 XML 自定义 SQL。
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 游标分页查询订单详情（延迟关联 + 多表join）
     */
    List<OrderDetailDTO> selectOrderDetailByCursor(@Param("cursor") long cursor,
                                                    @Param("pageSize") int pageSize);

    /**
     * 带条件的游标分页查询
     */
    List<OrderDetailDTO> selectOrderDetailByCursorWithCondition(@Param("cursor") long cursor,
                                                                 @Param("pageSize") int pageSize,
                                                                 @Param("status") Integer status);

    /**
     * 传统 OFFSET 分页查询（用于混合分页的前几百页）
     */
    List<OrderDetailDTO> selectOrderDetailByOffset(@Param("offset") int offset,
                                                    @Param("pageSize") int pageSize,
                                                    @Param("status") Integer status);

    /**
     * 查询总记录数
     */
    long countOrders(@Param("status") Integer status);

    /**
     * 根据订单号更新订单状态
     */
    int updateStatusByOrderNo(@Param("orderNo") String orderNo, @Param("status") int status);

    /**
     * 根据订单号查询订单详情
     */
    OrderDetailDTO selectOrderDetailByOrderNo(@Param("orderNo") String orderNo);
}
