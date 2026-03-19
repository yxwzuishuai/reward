USE `api-pro`;

-- ========== 批量生成订单数据 ==========
-- 使用存储过程 + 批量INSERT，快速生成大量订单
-- 每次插入1000条，循环执行，总共生成约100万条（演示用，生产可调大）

DELIMITER $$
DROP PROCEDURE IF EXISTS gen_orders$$
CREATE PROCEDURE gen_orders(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE batch_size INT DEFAULT 1000;
    DECLARE batch_count INT;
    DECLARE j INT;
    DECLARE v_user_id BIGINT;
    DECLARE v_product_id BIGINT;
    DECLARE v_shop_id BIGINT;
    DECLARE v_amount DECIMAL(12,2);
    DECLARE v_quantity INT;
    DECLARE v_status TINYINT;
    DECLARE v_days_ago INT;

    SET batch_count = total / batch_size;

    -- 关闭自动提交，大幅提升插入速度
    SET autocommit = 0;

    WHILE i < batch_count DO
        SET j = 0;
        START TRANSACTION;
        WHILE j < batch_size DO
            -- 随机生成订单数据
            SET v_user_id = FLOOR(1 + RAND() * 100);
            SET v_product_id = FLOOR(1 + RAND() * 50);
            SET v_quantity = FLOOR(1 + RAND() * 5);
            SET v_status = FLOOR(RAND() * 5);
            SET v_days_ago = FLOOR(RAND() * 365);

            -- 从商品表取价格和店铺（这里简化处理，直接用随机值）
            SET v_shop_id = FLOOR(1 + RAND() * 20);
            SET v_amount = ROUND(10 + RAND() * 10000, 2) * v_quantity;

            INSERT INTO orders (order_no, user_id, product_id, shop_id, amount, quantity, status, pay_time, create_time)
            VALUES (
                -- 订单号：日期 + 序号 + 随机数
                CONCAT(DATE_FORMAT(DATE_SUB(NOW(), INTERVAL v_days_ago DAY), '%Y%m%d'),
                       LPAD(i * batch_size + j, 8, '0'),
                       LPAD(FLOOR(RAND() * 10000), 4, '0')),
                v_user_id,
                v_product_id,
                v_shop_id,
                v_amount,
                v_quantity,
                v_status,
                -- 已支付及之后的状态才有支付时间
                IF(v_status >= 1,
                   DATE_SUB(NOW(), INTERVAL v_days_ago DAY) + INTERVAL FLOOR(RAND() * 3600) SECOND,
                   NULL),
                DATE_SUB(NOW(), INTERVAL v_days_ago DAY) - INTERVAL FLOOR(RAND() * 86400) SECOND
            );

            SET j = j + 1;
        END WHILE;
        COMMIT;
        SET i = i + 1;
    END WHILE;

    SET autocommit = 1;
END$$
DELIMITER ;

-- 生成50万条订单数据（大约需要1-2分钟）
-- 如果需要更多数据，可以多次调用或调大参数
CALL gen_orders(500000);

-- 查看数据量
SELECT COUNT(*) AS total_orders FROM orders;
SELECT status, COUNT(*) AS cnt FROM orders GROUP BY status ORDER BY status;

DROP PROCEDURE IF EXISTS gen_orders;
