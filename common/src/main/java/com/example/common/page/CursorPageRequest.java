package com.example.common.page;

import lombok.Data;

/**
 * 游标分页请求参数
 * <p>
 * 深分页解决方案：使用上一页最后一条记录的ID作为游标，
 * 替代传统的 OFFSET 分页，避免 MySQL 在大偏移量时扫描大量无用行。
 * <p>
 * 传统分页: SELECT * FROM t LIMIT 10000000, 10  -- 需要扫描1000万行再丢弃
 * 游标分页: SELECT * FROM t WHERE id > lastId LIMIT 10  -- 直接从索引定位，极快
 */
@Data
public class CursorPageRequest {

    /**
     * 游标值：上一页最后一条记录的ID
     * 首次查询传 null 或 0
     */
    private Long cursor;

    /**
     * 每页大小，默认20
     */
    private Integer pageSize = 20;

    /**
     * 获取安全的游标值
     */
    public long getSafeCursor() {
        return cursor == null || cursor < 0 ? 0L : cursor;
    }

    /**
     * 获取安全的每页大小，限制最大100防止一次查太多
     */
    public int getSafePageSize() {
        if (pageSize == null || pageSize <= 0) return 20;
        return Math.min(pageSize, 100);
    }
}
