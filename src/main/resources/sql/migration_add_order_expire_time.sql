SET @db = DATABASE();

SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'mall_order' AND COLUMN_NAME = 'expire_time';
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE mall_order ADD COLUMN expire_time DATETIME NULL COMMENT ''latest payment deadline'' AFTER remark',
  'SELECT ''skip mall_order.expire_time''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @idx_exists
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'mall_order' AND INDEX_NAME = 'idx_status_expire_time';
SET @sql = IF(@idx_exists = 0,
  'ALTER TABLE mall_order ADD INDEX idx_status_expire_time(status, expire_time)',
  'SELECT ''skip mall_order.idx_status_expire_time''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
