package com.example.common.page;

import lombok.Data;

import java.util.List;
import java.util.function.Function;

/**
 * 混合分页响应结果
 */
@Data
public class HybridPageResult<T> {

    private List<T> records;

    /**
     * 下一页游标（本页最后一条记录的ID）
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

    /**
     * 当前使用的分页模式
     */
    private String mode;

    /**
     * 总记录数（仅 OFFSET 模式下返回，用于前端计算总页数）
     */
    private Long total;

    /**
     * 当前页码（仅 OFFSET 模式下有意义）
     */
    private Integer pageNum;

    public static <T> HybridPageResult<T> ofOffset(List<T> records, int pageNum, int pageSize,
                                                    long total, Function<T, Long> idExtractor) {
        HybridPageResult<T> result = new HybridPageResult<>();
        result.setRecords(records);
        result.setSize(records.size());
        result.setMode("offset");
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setHasMore((long) pageNum * pageSize < total);
        if (!records.isEmpty()) {
            result.setNextCursor(idExtractor.apply(records.get(records.size() - 1)));
        }
        return result;
    }

    public static <T> HybridPageResult<T> ofCursor(List<T> records, int pageSize,
                                                    Function<T, Long> idExtractor) {
        HybridPageResult<T> result = new HybridPageResult<>();
        result.setRecords(records);
        result.setSize(records.size());
        result.setMode("cursor");
        result.setHasMore(records.size() >= pageSize);
        if (!records.isEmpty()) {
            result.setNextCursor(idExtractor.apply(records.get(records.size() - 1)));
        }
        return result;
    }
}
