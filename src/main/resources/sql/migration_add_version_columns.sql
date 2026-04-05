-- Add optimistic-lock version columns for mutable business tables.
-- Compatible with MySQL versions that do not support `ADD COLUMN IF NOT EXISTS`.
-- Execute once in each business database (for example: kkmall and kkmall-dev).

SET @db = DATABASE();

-- mall_product.version
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'mall_product' AND COLUMN_NAME = 'version';
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE mall_product ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT ''optimistic lock version'' AFTER images',
  'SELECT ''skip mall_product.version''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- mall_category.version
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'mall_category' AND COLUMN_NAME = 'version';
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE mall_category ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT ''optimistic lock version'' AFTER icon',
  'SELECT ''skip mall_category.version''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- mall_coupon.version
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'mall_coupon' AND COLUMN_NAME = 'version';
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE mall_coupon ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT ''optimistic lock version'' AFTER status',
  'SELECT ''skip mall_coupon.version''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- mall_user.version
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'mall_user' AND COLUMN_NAME = 'version';
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE mall_user ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT ''optimistic lock version'' AFTER points',
  'SELECT ''skip mall_user.version''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- mall_order.version
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'mall_order' AND COLUMN_NAME = 'version';
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE mall_order ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT ''optimistic lock version'' AFTER confirm_time',
  'SELECT ''skip mall_order.version''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- mall_payment.version
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'mall_payment' AND COLUMN_NAME = 'version';
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE mall_payment ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT ''optimistic lock version'' AFTER third_party_no',
  'SELECT ''skip mall_payment.version''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- mall_refund.version
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'mall_refund' AND COLUMN_NAME = 'version';
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE mall_refund ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT ''optimistic lock version'' AFTER refund_time',
  'SELECT ''skip mall_refund.version''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- mall_user_coupon.version
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'mall_user_coupon' AND COLUMN_NAME = 'version';
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE mall_user_coupon ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT ''optimistic lock version'' AFTER expire_time',
  'SELECT ''skip mall_user_coupon.version''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- mall_address.version
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'mall_address' AND COLUMN_NAME = 'version';
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE mall_address ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT ''optimistic lock version'' AFTER is_default',
  'SELECT ''skip mall_address.version''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- mall_cart.version
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'mall_cart' AND COLUMN_NAME = 'version';
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE mall_cart ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT ''optimistic lock version'' AFTER selected',
  'SELECT ''skip mall_cart.version''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
