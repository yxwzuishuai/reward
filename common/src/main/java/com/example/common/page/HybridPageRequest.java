package com.example.common.page;

import lombok.Data;

/**
 * 混合分页请求参数
 * <p>
 * 策略：前 N 页用传统 OFFSET 分页（支持跳页），超过阈值自动切换为游标分页（保证性能）。
 * 阈值默认 500 页，即 offset <= 500 * pageSize 时走 OFFSET，否则走游标。
 */
@Data
public class HybridPageRequest {

    /**
     * 当前页码（从1开始），用于传统分页
     */
    private Integer pageNum;

    /**
     * 游标值：上一页最后一条记录的ID，用于游标分页
     */
    private Long cursor;

    /**
     * 每页大小，默认20
     */
    private Integer pageSize = 20;

    /**
     * OFFSET 分页的最大页码阈值，超过此值自动切换为游标分页
     */
    private static final int MAX_OFFSET_PAGE = 500;

    public int getSafePageSize() {
        if (pageSize == null || pageSize <= 0) return 20;
        return Math.min(pageSize, 100);
    }

    public int getSafePageNum() {
        return (pageNum == null || pageNum < 1) ? 1 : pageNum;
    }

    public long getSafeCursor() {
        return (cursor == null || cursor < 0) ? 0L : cursor;
    }

    /**
     * 判断是否应该使用游标分页
     * 条件：页码超过阈值，或者前端主动传了 cursor
     */
    public boolean shouldUseCursor() {
        if (cursor != null && cursor > 0) {
            return true;
        }
        return getSafePageNum() > MAX_OFFSET_PAGE;
    }

    /**
     * 计算 OFFSET 值
     */
    public int getOffset() {
        return (getSafePageNum() - 1) * getSafePageSize();
    }
}
