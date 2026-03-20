-- 修复 deleted 字段类型和默认值
-- 将 TINYINT DEFAULT 0 改为 BIGINT DEFAULT NULL

-- 1. 修改 mall_user 表
ALTER TABLE `mall_user`
MODIFY COLUMN `deleted` BIGINT DEFAULT NULL COMMENT '逻辑删除（NULL-未删除，时间戳-已删除）';

-- 更新现有数据：将 0 改为 NULL
UPDATE `mall_user` SET `deleted` = NULL WHERE `deleted` = 0;

-- 2. 修改 mall_address 表
ALTER TABLE `mall_address`
MODIFY COLUMN `deleted` BIGINT DEFAULT NULL COMMENT '逻辑删除（NULL-未删除，时间戳-已删除）';

UPDATE `mall_address` SET `deleted` = NULL WHERE `deleted` = 0;

-- 3. 修改 mall_order 表
ALTER TABLE `mall_order`
MODIFY COLUMN `deleted` BIGINT DEFAULT NULL COMMENT '逻辑删除（NULL-未删除，时间戳-已删除）';

UPDATE `mall_order` SET `deleted` = NULL WHERE `deleted` = 0;

-- 4. 修改 mall_category 表
ALTER TABLE `mall_category`
MODIFY COLUMN `deleted` BIGINT DEFAULT NULL COMMENT '逻辑删除（NULL-未删除，时间戳-已删除）';

UPDATE `mall_category` SET `deleted` = NULL WHERE `deleted` = 0;