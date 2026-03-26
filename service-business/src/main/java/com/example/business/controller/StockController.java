package com.example.business.controller;

import com.example.business.service.StockService;
import com.example.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 库存接口
 */
@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    /**
     * 初始化库存
     */
    @PostMapping("/init")
    public R<Boolean> initStock(@RequestParam Long productId,
                                @RequestParam int stock,
                                @RequestParam(defaultValue = "0") long expireSec) {
        return R.ok(stockService.initStock(productId, stock, expireSec));
    }

    /**
     * 扣减库存
     */
    @PostMapping("/deduct")
    public R<Long> deductStock(@RequestParam Long productId,
                               @RequestParam(defaultValue = "1") int quantity) {
        return R.ok(stockService.deductStock(productId, quantity));
    }

    /**
     * 恢复库存
     */
    @PostMapping("/restore")
    public R<Long> restoreStock(@RequestParam Long productId,
                                @RequestParam(defaultValue = "1") int quantity) {
        return R.ok(stockService.restoreStock(productId, quantity));
    }

    /**
     * 查询库存
     */
    @GetMapping("/{productId}")
    public R<Long> getStock(@PathVariable Long productId) {
        return R.ok(stockService.getStock(productId));
    }

    /**
     * 异步查询单个商品库存
     */
    @GetMapping("/async/{productId}")
    public CompletableFuture<R<Long>> getStockAsync(@PathVariable Long productId) {
        return stockService.getStockAsync(productId).thenApply(R::ok);
    }

    /**
     * 批量异步查询多个商品库存
     * 示例: GET /stock/batch?productIds=1001,1002,1003
     */
    @GetMapping("/batch")
    public CompletableFuture<R<Map<Long, Long>>> batchGetStock(@RequestParam List<Long> productIds) {
        return stockService.batchGetStockAsync(productIds).thenApply(R::ok);
    }
}
