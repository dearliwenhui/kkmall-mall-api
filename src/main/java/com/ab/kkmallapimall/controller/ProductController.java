package com.ab.kkmallapimall.controller;

import com.ab.kkmallapimall.common.PageResult;
import com.ab.kkmallapimall.common.Result;
import com.ab.kkmallapimall.dto.response.ProductVO;
import com.ab.kkmallapimall.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品控制器
 */
@Tag(name = "商品管理", description = "商品浏览、搜索等接口")
@RestController
@RequestMapping("/api/mall/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "获取商品列表")
    @GetMapping
    public Result<PageResult<ProductVO>> getProductList(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<ProductVO> result = productService.getProductList(categoryId, keyword, pageNum, pageSize);
        return Result.success(result);
    }

    @Operation(summary = "获取商品详情")
    @GetMapping("/{id}")
    public Result<ProductVO> getProductDetail(@PathVariable Long id) {
        ProductVO product = productService.getProductDetail(id);
        return Result.success(product);
    }

    @Operation(summary = "获取热门商品")

    @GetMapping("/hot")
    public Result<List<ProductVO>> getHotProducts(@RequestParam(defaultValue = "10") Integer limit) {
        List<ProductVO> products = productService.getHotProducts(limit);
        return Result.success(products);
    }

    @Operation(summary = "搜索商品")
    @GetMapping("/search")
    public Result<PageResult<ProductVO>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<ProductVO> result = productService.getProductList(null, keyword, pageNum, pageSize);
        return Result.success(result);
    }
}
