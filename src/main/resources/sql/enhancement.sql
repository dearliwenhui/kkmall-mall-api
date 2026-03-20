-- KKMall 商城系统功能增强 SQL 脚本
-- 创建时间: 2026-03-18
-- 说明: 包含支付、评价、退款、优惠券、积分、收藏、搜索历史等功能的数据库表
-- 注意: 本脚本可以安全地重复执行

-- ============================================
-- 1. 支付记录表 (P0)
-- ============================================
CREATE TABLE IF NOT EXISTS `mall_payment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `payment_no` VARCHAR(64) NOT NULL COMMENT '支付流水号',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
  `payment_type` TINYINT NOT NULL COMMENT '支付方式（1-支付宝，2-微信，3-模拟支付）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态（0-待支付，1-支付成功，2-支付失败）',
  `pay_time` DATETIME COMMENT '支付时间',
  `third_party_no` VARCHAR(100) COMMENT '第三方支付流水号',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';

-- ============================================
-- 2. 订单表新增物流字段 (P0)
-- ============================================
-- 检查并添加 logistics_company 字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'mall_order'
  AND COLUMN_NAME = 'logistics_company';

SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `mall_order` ADD COLUMN `logistics_company` VARCHAR(50) COMMENT ''物流公司'' AFTER `receiver_address`',
  'SELECT ''logistics_company already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 tracking_number 字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'mall_order'
  AND COLUMN_NAME = 'tracking_number';

SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `mall_order` ADD COLUMN `tracking_number` VARCHAR(100) COMMENT ''物流单号'' AFTER `logistics_company`',
  'SELECT ''tracking_number already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 ship_time 字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'mall_order'
  AND COLUMN_NAME = 'ship_time';

SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `mall_order` ADD COLUMN `ship_time` DATETIME COMMENT ''发货时间'' AFTER `tracking_number`',
  'SELECT ''ship_time already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- 3. 商品评价表 (P1)
-- ============================================
CREATE TABLE IF NOT EXISTS `mall_review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `order_item_id` BIGINT NOT NULL COMMENT '订单明细ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `rating` TINYINT NOT NULL COMMENT '评分（1-5星）',
  `content` TEXT COMMENT '评价内容',
  `images` TEXT COMMENT '评价图片（逗号分隔）',
  `reply` TEXT COMMENT '商家回复',
  `reply_time` DATETIME COMMENT '回复时间',
  `status` TINYINT DEFAULT 1 COMMENT '状态（0-隐藏，1-显示）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_order_item_id` (`order_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评价表';

-- ============================================
-- 4. 退款申请表 (P2)
-- ============================================
CREATE TABLE IF NOT EXISTS `mall_refund` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `refund_no` VARCHAR(64) NOT NULL COMMENT '退款单号',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `refund_amount` DECIMAL(10,2) NOT NULL COMMENT '退款金额',
  `refund_type` TINYINT NOT NULL COMMENT '退款类型（1-仅退款，2-退货退款）',
  `reason` VARCHAR(200) COMMENT '退款原因',
  `description` TEXT COMMENT '问题描述',
  `images` TEXT COMMENT '凭证图片（逗号分隔）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0-待审核，1-审核通过，2-审核拒绝，3-退款成功）',
  `reject_reason` VARCHAR(200) COMMENT '拒绝原因',
  `refund_time` DATETIME COMMENT '退款时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款申请表';

-- ============================================
-- 5. 优惠券表 (P2)
-- ============================================
CREATE TABLE IF NOT EXISTS `mall_coupon` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(100) NOT NULL COMMENT '优惠券名称',
  `type` TINYINT NOT NULL COMMENT '类型（1-满减券，2-折扣券）',
  `discount_amount` DECIMAL(10,2) COMMENT '优惠金额',
  `discount_rate` DECIMAL(3,2) COMMENT '折扣率（0.8表示8折）',
  `min_amount` DECIMAL(10,2) COMMENT '最低消费金额',
  `total_count` INT COMMENT '发行总量',
  `received_count` INT DEFAULT 0 COMMENT '已领取数量',
  `valid_days` INT COMMENT '有效天数',
  `start_time` DATETIME COMMENT '开始时间',
  `end_time` DATETIME COMMENT '结束时间',
  `status` TINYINT DEFAULT 1 COMMENT '状态（0-禁用，1-启用）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券表';

-- ============================================
-- 6. 用户优惠券表 (P2)
-- ============================================
CREATE TABLE IF NOT EXISTS `mall_user_coupon` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `coupon_id` BIGINT NOT NULL COMMENT '优惠券ID',
  `status` TINYINT DEFAULT 0 COMMENT '状态（0-未使用，1-已使用，2-已过期）',
  `used_time` DATETIME COMMENT '使用时间',
  `order_id` BIGINT COMMENT '订单ID',
  `expire_time` DATETIME COMMENT '过期时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_coupon_id` (`coupon_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券表';

-- ============================================
-- 7. 用户表新增积分字段 (P2)
-- ============================================
-- 检查并添加 points 字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'mall_user'
  AND COLUMN_NAME = 'points';

SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `mall_user` ADD COLUMN `points` INT DEFAULT 0 COMMENT ''积分余额'' AFTER `status`',
  'SELECT ''points already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- 8. 积分记录表 (P2)
-- ============================================
CREATE TABLE IF NOT EXISTS `mall_points_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `points` INT NOT NULL COMMENT '积分变动（正数为增加，负数为减少）',
  `type` TINYINT NOT NULL COMMENT '类型（1-签到，2-购物，3-评价，4-兑换）',
  `order_id` BIGINT COMMENT '关联订单ID',
  `description` VARCHAR(200) COMMENT '描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分记录表';

-- ============================================
-- 9. 商品收藏表 (P2)
-- ============================================
CREATE TABLE IF NOT EXISTS `mall_favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品收藏表';

-- ============================================
-- 10. 搜索历史表 (P2)
-- ============================================
CREATE TABLE IF NOT EXISTS `mall_search_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `keyword` VARCHAR(100) NOT NULL COMMENT '搜索关键词',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_keyword` (`keyword`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索历史表';

-- ============================================
-- 11. 订单表新增优惠券字段
-- ============================================
-- 检查并添加 coupon_id 字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'mall_order'
  AND COLUMN_NAME = 'coupon_id';

SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `mall_order` ADD COLUMN `coupon_id` BIGINT COMMENT ''使用的优惠券ID'' AFTER `total_amount`',
  'SELECT ''coupon_id already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 coupon_amount 字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'mall_order'
  AND COLUMN_NAME = 'coupon_amount';

SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `mall_order` ADD COLUMN `coupon_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT ''优惠券优惠金额'' AFTER `coupon_id`',
  'SELECT ''coupon_amount already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- 插入测试数据
-- ============================================

-- 插入优惠券测试数据（如果不存在）
INSERT INTO `mall_coupon` (`name`, `type`, `discount_amount`, `min_amount`, `total_count`, `received_count`, `valid_days`, `start_time`, `end_time`, `status`)
SELECT '新人专享券', 1, 10.00, 50.00, 1000, 0, 30, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1
WHERE NOT EXISTS (SELECT 1 FROM `mall_coupon` WHERE `name` = '新人专享券');

INSERT INTO `mall_coupon` (`name`, `type`, `discount_amount`, `min_amount`, `total_count`, `received_count`, `valid_days`, `start_time`, `end_time`, `status`)
SELECT '满100减20', 1, 20.00, 100.00, 500, 0, 15, NOW(), DATE_ADD(NOW(), INTERVAL 15 DAY), 1
WHERE NOT EXISTS (SELECT 1 FROM `mall_coupon` WHERE `name` = '满100减20');

INSERT INTO `mall_coupon` (`name`, `type`, `discount_amount`, `min_amount`, `total_count`, `received_count`, `valid_days`, `start_time`, `end_time`, `status`)
SELECT '全场8折券', 2, NULL, 0.00, 200, 0, 7, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 1
WHERE NOT EXISTS (SELECT 1 FROM `mall_coupon` WHERE `name` = '全场8折券');

-- 完成
SELECT '数据库增强脚本执行完成！' AS message;