package com.example.common.page;

import lombok.Data;
import java.util.List;

/**
 * 游标分页响应结果
 */
@Data
public class CursorPageResult<T> {

    /**
     * 数据列表
     */
    private List<T> records;

    /**
     * 下一页游标（本页最后一条记录的ID）
     * 为 null 表示没有下一页了
     */
    private Long nextCursor;

    /**
     * 是否还有下一页
     */
    private boolean hasMore;

    /**
     * 本页实际记录数
     */
    private int size;

    public static <T> CursorPageResult<T> of(List<T> records, int pageSize, java.util.function.Function<T, Long> idExtractor) {
        CursorPageResult<T> result = new CursorPageResult<>();
        result.setRecords(records);
        result.setSize(records.size());
        // 如果查出的数据量等于pageSize，说明可能还有下一页
        result.setHasMore(records.size() >= pageSize);
        if (!records.isEmpty()) {
            // 取最后一条记录的ID作为下一页游标
            result.setNextCursor(idExtractor.apply(records.get(records.size() - 1)));
        }
        return result;
    }
}
