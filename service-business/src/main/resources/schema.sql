-- ============================================
-- 深分页优化方案 - 建表及索引参考
-- ============================================

-- 订单主表（3000万+数据量）
CREATE TABLE orders (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_no    VARCHAR(64)  NOT NULL COMMENT '订单号',
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    product_id  BIGINT       NOT NULL COMMENT '商品ID',
    shop_id     BIGINT       NOT NULL COMMENT '店铺ID',
    amount      DECIMAL(12,2) NOT NULL COMMENT '订单金额',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '订单状态: 0-待支付 1-已支付 2-已发货 3-已完成 4-已取消',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    -- 游标分页核心索引：主键ID本身就是聚簇索引，天然有序
    -- 子查询 SELECT id FROM orders WHERE id > ? LIMIT ? 走覆盖索引，不回表
    KEY idx_order_no (order_no),
    KEY idx_user_id (user_id),
    -- 带条件查询时的联合索引
    -- 查询: WHERE status = ? AND id > ? ORDER BY id LIMIT ?
    -- 索引顺序很重要：等值条件在前，范围条件在后
    KEY idx_status_id (status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 用户表
CREATE TABLE users (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    user_name VARCHAR(64) NOT NULL COMMENT '用户名',
    phone     VARCHAR(20) COMMENT '手机号',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 商品表
CREATE TABLE products (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    product_name VARCHAR(128) NOT NULL COMMENT '商品名称',
    category     VARCHAR(64)  COMMENT '商品分类',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 店铺表
CREATE TABLE shops (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    shop_name VARCHAR(128) NOT NULL COMMENT '店铺名称',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺表';

-- ============================================
-- 性能对比（3000万数据量）
-- ============================================
--
-- 传统分页（越往后越慢）：
--   SELECT * FROM orders JOIN ... LIMIT 0, 20        -- ~5ms
--   SELECT * FROM orders JOIN ... LIMIT 100000, 20   -- ~200ms
--   SELECT * FROM orders JOIN ... LIMIT 1000000, 20  -- ~2s
--   SELECT * FROM orders JOIN ... LIMIT 10000000, 20 -- ~30s
--   SELECT * FROM orders JOIN ... LIMIT 30000000, 20 -- ~90s
--
-- 游标分页（无论第几页都稳定）：
--   WHERE id > 0        LIMIT 20  -- ~3ms
--   WHERE id > 100000   LIMIT 20  -- ~3ms
--   WHERE id > 1000000  LIMIT 20  -- ~3ms
--   WHERE id > 10000000 LIMIT 20  -- ~3ms
--   WHERE id > 30000000 LIMIT 20  -- ~3ms
