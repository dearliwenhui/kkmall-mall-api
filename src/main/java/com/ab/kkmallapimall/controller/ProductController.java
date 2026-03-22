package com.ab.kkmallapimall.controller;

import com.ab.kkmallapimall.common.PageResult;
import com.ab.kkmallapimall.common.Result;
import com.ab.kkmallapimall.dto.response.ProductVO;
import com.ab.kkmallapimall.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Product controller.
 */
@Tag(name = "Product", description = "Product browse and search APIs")
@RestController
@RequestMapping("/api/mall/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Get product list")
    @GetMapping
    public Result<PageResult<ProductVO>> getProductList(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        PageResult<ProductVO> result = productService.getProductList(categoryId, keyword, sortBy, pageNum, pageSize);
        return Result.success(result);
    }

    @Operation(summary = "Get product detail")
    @GetMapping("/{id}")
    public Result<ProductVO> getProductDetail(@PathVariable Long id) {
        ProductVO product = productService.getProductDetail(id);
        return Result.success(product);
    }

    @Operation(summary = "Get hot products")
    @GetMapping("/hot")
    public Result<List<ProductVO>> getHotProducts(@RequestParam(defaultValue = "10") Integer limit) {
        List<ProductVO> products = productService.getHotProducts(limit);
        return Result.success(products);
    }

    @Operation(summary = "Search products")
    @GetMapping("/search")
    public Result<PageResult<ProductVO>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        PageResult<ProductVO> result = productService.getProductList(null, keyword, sortBy, pageNum, pageSize);
        return Result.success(result);
    }
}
