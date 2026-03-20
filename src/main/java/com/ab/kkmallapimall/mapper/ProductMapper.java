package com.ab.kkmallapimall.mapper;

import com.ab.kkmallapimall.entity.Product;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品Mapper（只读）
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
