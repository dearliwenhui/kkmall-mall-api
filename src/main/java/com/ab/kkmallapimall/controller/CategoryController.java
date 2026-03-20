package com.ab.kkmallapimall.controller;

import com.ab.kkmallapimall.common.Result;
import com.ab.kkmallapimall.dto.response.CategoryTreeVO;
import com.ab.kkmallapimall.entity.Category;
import com.ab.kkmallapimall.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类控制器
 */
@Tag(name = "分类管理", description = "商品分类查询接口")
@RestController
@RequestMapping("/api/mall/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "获取分类树")
    @GetMapping("/tree")
    public Result<List<CategoryTreeVO>> getCategoryTree() {
        List<CategoryTreeVO> tree = categoryService.getCategoryTree();
        return Result.success(tree);
    }

    @Operation(summary = "获取分类详情")
    @GetMapping("/{id}")
    public Result<Category> getCategoryDetail(@PathVariable Long id) {
        Category category = categoryService.getCategoryDetail(id);
        return Result.success(category);
    }
}
