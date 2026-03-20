package com.ab.kkmallapimall.controller;

import com.ab.kkmallapimall.common.PageResult;
import com.ab.kkmallapimall.common.Result;
import com.ab.kkmallapimall.service.FavoriteService;
import com.ab.kkmallapimall.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 收藏控制器
 */
@Tag(name = "商品收藏", description = "商品收藏相关接口")
@RestController
@RequestMapping("/api/mall/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "添加收藏")
    @PostMapping("/{productId}")
    public Result<Void> addFavorite(@PathVariable Long productId) {
        Long userId = securityUtils.getCurrentUserId();
        favoriteService.addFavorite(userId, productId);
        return Result.success("收藏成功", null);
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{productId}")
    public Result<Void> removeFavorite(@PathVariable Long productId) {
        Long userId = securityUtils.getCurrentUserId();
        favoriteService.removeFavorite(userId, productId);
        return Result.success("取消收藏成功", null);
    }

    @Operation(summary = "检查是否已收藏")
    @GetMapping("/check/{productId}")
    public Result<Boolean> isFavorite(@PathVariable Long productId) {
        Long userId = securityUtils.getCurrentUserId();
        boolean isFavorite = favoriteService.isFavorite(userId, productId);
        return Result.success(isFavorite);
    }

    @Operation(summary = "获取收藏列表")
    @GetMapping
    public Result<PageResult<Map<String, Object>>> getFavoriteList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = securityUtils.getCurrentUserId();
        PageResult<Map<String, Object>> result = favoriteService.getFavoriteList(userId, pageNum, pageSize);
        return Result.success(result);
    }
}
