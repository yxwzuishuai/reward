-- 创建数据库
CREATE DATABASE IF NOT EXISTS `api-pro` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `api-pro`;

-- 用户表
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    user_name VARCHAR(64) NOT NULL COMMENT '用户名',
    phone     VARCHAR(20) COMMENT '手机号',
    email     VARCHAR(64) COMMENT '邮箱',
    create_time DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 店铺表
DROP TABLE IF EXISTS shops;
CREATE TABLE shops (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    shop_name VARCHAR(128) NOT NULL COMMENT '店铺名称',
    address   VARCHAR(256) COMMENT '店铺地址',
    create_time DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺表';

-- 商品表
DROP TABLE IF EXISTS products;
CREATE TABLE products (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    product_name VARCHAR(128)  NOT NULL COMMENT '商品名称',
    category     VARCHAR(64)   COMMENT '商品分类',
    price        DECIMAL(12,2) NOT NULL COMMENT '商品价格',
    shop_id      BIGINT        NOT NULL COMMENT '所属店铺',
    create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_shop_id (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 订单表（主表，模拟大数据量）
DROP TABLE IF EXISTS orders;
CREATE TABLE orders (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_no    VARCHAR(64)   NOT NULL COMMENT '订单号',
    user_id     BIGINT        NOT NULL COMMENT '用户ID',
    product_id  BIGINT        NOT NULL COMMENT '商品ID',
    shop_id     BIGINT        NOT NULL COMMENT '店铺ID',
    amount      DECIMAL(12,2) NOT NULL COMMENT '订单金额',
    quantity    INT           NOT NULL DEFAULT 1 COMMENT '购买数量',
    status      TINYINT       NOT NULL DEFAULT 0 COMMENT '0-待支付 1-已支付 2-已发货 3-已完成 4-已取消',
    pay_time    DATETIME      COMMENT '支付时间',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id),
    KEY idx_shop_id (shop_id),
    KEY idx_status_id (status, id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ========== 插入基础数据 ==========

-- 插入100个用户
INSERT INTO users (user_name, phone, email) VALUES
('张三', '13800001001', 'zhangsan@example.com'),
('李四', '13800001002', 'lisi@example.com'),
('王五', '13800001003', 'wangwu@example.com'),
('赵六', '13800001004', 'zhaoliu@example.com'),
('孙七', '13800001005', 'sunqi@example.com'),
('周八', '13800001006', 'zhouba@example.com'),
('吴九', '13800001007', 'wujiu@example.com'),
('郑十', '13800001008', 'zhengshi@example.com'),
('陈一', '13800001009', 'chenyi@example.com'),
('林二', '13800001010', 'liner@example.com');

-- 用存储过程批量生成更多用户
DELIMITER $$
DROP PROCEDURE IF EXISTS gen_users$$
CREATE PROCEDURE gen_users()
BEGIN
    DECLARE i INT DEFAULT 11;
    WHILE i <= 100 DO
        INSERT INTO users (user_name, phone, email)
        VALUES (CONCAT('用户', i), CONCAT('138', LPAD(i, 8, '0')), CONCAT('user', i, '@example.com'));
        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;
CALL gen_users();
DROP PROCEDURE IF EXISTS gen_users;

-- 插入20个店铺
INSERT INTO shops (shop_name, address) VALUES
('天猫旗舰店', '杭州市余杭区'),
('京东自营店', '北京市朝阳区'),
('苏宁易购店', '南京市玄武区'),
('拼多多优选', '上海市长宁区'),
('唯品会特卖', '广州市天河区'),
('网易严选店', '杭州市滨江区'),
('小米官方店', '北京市海淀区'),
('华为旗舰店', '深圳市龙岗区'),
('OPPO官方店', '东莞市长安镇'),
('vivo官方店', '东莞市长安镇'),
('联想专卖店', '北京市海淀区'),
('戴尔直销店', '厦门市湖里区'),
('耐克官方店', '上海市静安区'),
('阿迪达斯店', '上海市黄浦区'),
('优衣库旗舰', '上海市浦东新区'),
('三只松鼠店', '芜湖市弋江区'),
('良品铺子店', '武汉市东西湖区'),
('百草味旗舰', '杭州市余杭区'),
('海底捞外卖', '北京市朝阳区'),
('星巴克外送', '上海市静安区');

-- 插入50个商品
INSERT INTO products (product_name, category, price, shop_id) VALUES
('iPhone 15 Pro Max', '手机', 9999.00, 1),
('MacBook Pro 14寸', '电脑', 14999.00, 1),
('AirPods Pro 2', '耳机', 1799.00, 1),
('iPad Air 5', '平板', 4799.00, 1),
('Apple Watch S9', '手表', 2999.00, 1),
('小米14 Ultra', '手机', 5999.00, 7),
('小米电视75寸', '电视', 3999.00, 7),
('小米手环8', '手环', 249.00, 7),
('Redmi K70 Pro', '手机', 3299.00, 7),
('小米路由器AX9000', '路由器', 999.00, 7),
('华为Mate 60 Pro', '手机', 6999.00, 8),
('华为MatePad Pro', '平板', 4999.00, 8),
('华为Watch GT4', '手表', 1488.00, 8),
('华为MateBook X Pro', '电脑', 11999.00, 8),
('华为FreeBuds Pro 3', '耳机', 1199.00, 8),
('OPPO Find X7 Ultra', '手机', 5999.00, 9),
('vivo X100 Pro', '手机', 4999.00, 10),
('联想ThinkPad X1', '电脑', 9999.00, 11),
('戴尔XPS 15', '电脑', 12999.00, 12),
('耐克Air Max 270', '运动鞋', 899.00, 13),
('耐克Dunk Low', '运动鞋', 799.00, 13),
('阿迪达斯Ultraboost', '运动鞋', 1099.00, 14),
('优衣库联名T恤', '服装', 149.00, 15),
('优衣库牛仔裤', '服装', 199.00, 15),
('三只松鼠坚果礼盒', '零食', 168.00, 16),
('三只松鼠每日坚果', '零食', 89.00, 16),
('良品铺子肉脯', '零食', 39.90, 17),
('良品铺子蛋糕', '零食', 29.90, 17),
('百草味果干', '零食', 49.90, 18),
('百草味牛肉干', '零食', 59.90, 18),
('海底捞自热火锅', '食品', 39.90, 19),
('海底捞火锅底料', '食品', 15.90, 19),
('星巴克拿铁', '饮品', 38.00, 20),
('星巴克美式', '饮品', 32.00, 20),
('星巴克星冰乐', '饮品', 42.00, 20),
('京东E卡100元', '卡券', 100.00, 2),
('京东E卡500元', '卡券', 500.00, 2),
('苏宁易购洗衣机', '家电', 2999.00, 3),
('苏宁易购空调', '家电', 3999.00, 3),
('苏宁易购冰箱', '家电', 4999.00, 3),
('拼多多百亿补贴手机', '手机', 2999.00, 4),
('唯品会品牌女装', '服装', 299.00, 5),
('唯品会品牌男装', '服装', 399.00, 5),
('网易严选枕头', '家居', 199.00, 6),
('网易严选床垫', '家居', 1999.00, 6),
('网易严选毛巾', '家居', 49.00, 6),
('小米充电宝', '配件', 99.00, 7),
('华为充电器', '配件', 149.00, 8),
('AirTag 4件装', '配件', 779.00, 1),
('Apple Pencil 2', '配件', 999.00, 1);
