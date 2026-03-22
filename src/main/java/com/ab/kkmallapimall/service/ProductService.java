package com.ab.kkmallapimall.service;

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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
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
    public PageResult<ProductVO> getProductList(
            Long categoryId,
            String keyword,
            String sortBy,
            Integer pageNum,
            Integer pageSize
    ) {
        Page<Product> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1)
                .eq(categoryId != null, Product::getCategoryId, categoryId)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(Product::getProductName, keyword)
                        .or()
                        .like(Product::getDescription, keyword));

        if ("price".equalsIgnoreCase(sortBy)) {
            wrapper.orderByAsc(Product::getPrice).orderByDesc(Product::getCreateTime);
        } else {
            wrapper.orderByDesc(Product::getCreateTime);
        }

        Page<Product> productPage = productMapper.selectPage(page, wrapper);

        List<ProductVO> voList = productPage.getRecords().stream()
                .map(this::convertToVO)
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
        return convertToVO(product);
    }

    /**
     * Get hot products.
     */
    public List<ProductVO> getHotProducts(Integer limit) {
        Page<Product> page = new Page<>(1, limit);

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1)
                .orderByDesc(Product::getCreateTime);

        Page<Product> productPage = productMapper.selectPage(page, wrapper);

        return productPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    private ProductVO convertToVO(Product product) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);

        if (StringUtils.hasText(product.getImages())) {
            List<String> imageList = Arrays.asList(product.getImages().split(","));
            vo.setImageList(imageList);
            vo.setMainImage(imageList.isEmpty() ? null : imageList.get(0));
        }

        if (product.getCategoryId() != null) {
            Category category = categoryMapper.selectById(product.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }

        return vo;
    }
}
