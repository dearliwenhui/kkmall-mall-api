package com.ab.kkmallapimall.service;

import com.ab.kkmallapimall.common.Constants;
import com.ab.kkmallapimall.common.PageResult;
import com.ab.kkmallapimall.dto.response.ProductVO;
import com.ab.kkmallapimall.entity.Category;
import com.ab.kkmallapimall.entity.Product;
import com.ab.kkmallapimall.exception.BusinessException;
import com.ab.kkmallapimall.exception.ErrorCode;
import com.ab.kkmallapimall.mapper.CategoryMapper;
import com.ab.kkmallapimall.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Product service.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;

    /**
     * Get product list.
     */
    @Cacheable(cacheNames = Constants.Cache.PRODUCT_LIST, keyGenerator = "productListCacheKeyGenerator")
    public PageResult<ProductVO> getProductList(
            Long categoryId,
            String keyword,
            String sortBy,
            Integer pageNum,
            Integer pageSize
    ) {
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedSortBy = normalizeSortBy(sortBy);

        Page<Product> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1)
                .eq(categoryId != null, Product::getCategoryId, categoryId)
                .like(StringUtils.hasText(normalizedKeyword), Product::getProductName, normalizedKeyword);

        if ("price".equals(normalizedSortBy)) {
            wrapper.orderByAsc(Product::getPrice).orderByDesc(Product::getCreateTime);
        } else {
            wrapper.orderByDesc(Product::getCreateTime);
        }

        Page<Product> productPage = productMapper.selectPage(page, wrapper);
        Map<Long, String> categoryNameMap = buildCategoryNameMap(productPage.getRecords());

        List<ProductVO> voList = productPage.getRecords().stream()
                .map(product -> convertToVO(product, categoryNameMap))
                .collect(Collectors.toList());

        return PageResult.of(pageNum, pageSize, productPage.getTotal(), voList);
    }

    /**
     * Get product detail.
     */
    public ProductVO getProductDetail(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getStatus() == 0) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return convertToVO(product, buildCategoryNameMap(List.of(product)));
    }

    /**
     * Get hot products.
     */
    @Cacheable(cacheNames = Constants.Cache.PRODUCT_HOT, keyGenerator = "hotProductsCacheKeyGenerator")
    public List<ProductVO> getHotProducts(Integer limit) {
        int safeLimit = normalizeHotLimit(limit);

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1)
                .orderByDesc(Product::getCreateTime)
                .last("LIMIT " + safeLimit);

        List<Product> products = productMapper.selectList(wrapper);
        Map<Long, String> categoryNameMap = buildCategoryNameMap(products);

        return products.stream()
                .map(product -> convertToVO(product, categoryNameMap))
                .collect(Collectors.toList());
    }

    private ProductVO convertToVO(Product product, Map<Long, String> categoryNameMap) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);

        if (StringUtils.hasText(product.getImages())) {
            List<String> imageList = Arrays.asList(product.getImages().split(","));
            vo.setImageList(imageList);
            vo.setMainImage(imageList.isEmpty() ? null : imageList.get(0));
        }

        if (product.getCategoryId() != null) {
            vo.setCategoryName(categoryNameMap.get(product.getCategoryId()));
        }

        return vo;
    }

    private Map<Long, String> buildCategoryNameMap(List<Product> products) {
        Set<Long> categoryIds = products.stream()
                .map(Product::getCategoryId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (categoryIds.isEmpty()) {
            return Map.of();
        }

        return categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (left, right) -> left));
    }

    private int normalizeHotLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return 10;
        }
        return Math.min(limit, 50);
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim();
    }

    private String normalizeSortBy(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return null;
        }
        return sortBy.trim().toLowerCase();
    }
}
