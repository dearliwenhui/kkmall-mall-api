package com.ab.kkmallapimall.service;

import com.ab.kkmallapimall.common.PageResult;
import com.ab.kkmallapimall.entity.Favorite;
import com.ab.kkmallapimall.entity.Product;
import com.ab.kkmallapimall.exception.BusinessException;
import com.ab.kkmallapimall.exception.ErrorCode;
import com.ab.kkmallapimall.mapper.FavoriteMapper;
import com.ab.kkmallapimall.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 收藏服务
 */
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final ProductMapper productMapper;

    /**
     * 添加收藏
     */
    @Transactional(rollbackFor = Exception.class)
    public void addFavorite(Long userId, Long productId) {
        // 验证商品存在
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 检查是否已收藏
        Favorite existing = favoriteMapper.selectOne(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getProductId, productId)
        );
        if (existing != null) {
            throw new BusinessException(ErrorCode.FAVORITE_ALREADY_EXISTS);
        }

        // 添加收藏
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        favoriteMapper.insert(favorite);
    }

    /**
     * 取消收藏
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeFavorite(Long userId, Long productId) {
        favoriteMapper.delete(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getProductId, productId)
        );
    }

    /**
     * 检查是否已收藏
     */
    public boolean isFavorite(Long userId, Long productId) {
        Long count = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getProductId, productId)
        );
        return count > 0;
    }

    /**
     * 获取收藏列表
     */
    public PageResult<Map<String, Object>> getFavoriteList(Long userId, Integer pageNum, Integer pageSize) {
        Page<Favorite> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getCreateTime);

        Page<Favorite> favoritePage = favoriteMapper.selectPage(page, wrapper);

        List<Map<String, Object>> resultList = favoritePage.getRecords().stream()
                .map(favorite -> {
                    Map<String, Object> map = new HashMap<>();
                    Product product = productMapper.selectById(favorite.getProductId());
                    if (product != null) {
                        map.put("favoriteId", favorite.getId());
                        map.put("productId", product.getId());
                        map.put("productName", product.getProductName());
                        map.put("price", product.getPrice());
                        map.put("image", product.getImages() != null && product.getImages().contains(",")
                                ? product.getImages().split(",")[0]
                                : product.getImages());
                        map.put("createTime", favorite.getCreateTime());
                    }
                    return map;
                })
                .filter(map -> !map.isEmpty())
                .collect(Collectors.toList());

        return PageResult.of(pageNum, pageSize, favoritePage.getTotal(), resultList);
    }
}
