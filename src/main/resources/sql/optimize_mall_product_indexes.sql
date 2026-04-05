ALTER TABLE `mall_product`
  ADD INDEX `idx_mall_product_list` (`status`, `deleted`, `create_time`, `id`),
  ADD INDEX `idx_mall_product_category_list` (`status`, `deleted`, `category_id`, `create_time`, `id`),
  ADD INDEX `idx_mall_product_name` (`product_name`);
