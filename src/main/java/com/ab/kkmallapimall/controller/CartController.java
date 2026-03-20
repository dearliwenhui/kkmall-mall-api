package com.ab.kkmallapimall.controller;

import com.ab.kkmallapimall.common.Result;
import com.ab.kkmallapimall.dto.request.AddCartRequest;
import com.ab.kkmallapimall.dto.request.SelectItemsRequest;
import com.ab.kkmallapimall.dto.request.UpdateQuantityRequest;
import com.ab.kkmallapimall.dto.response.CartVO;
import com.ab.kkmallapimall.service.CartService;
import com.ab.kkmallapimall.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 购物车控制器
 */
@Tag(name = "购物车管理", description = "购物车相关接口")
@RestController
@RequestMapping("/api/mall/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "获取购物车列表")
    @GetMapping
    public Result<List<CartVO>> getCartList() {
        Long userId = securityUtils.getCurrentUserId();
        List<CartVO> cartList = cartService.getCartList(userId);
        return Result.success(cartList);
    }

    @Operation(summary = "添加到购物车")
    @PostMapping
    public Result<Void> addToCart(@Valid @RequestBody AddCartRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        cartService.addToCart(userId, request);
        return Result.success("添加成功", null);
    }

    @Operation(summary = "更新数量")
    @PutMapping("/{id}")
    public Result<Void> updateQuantity(
            @PathVariable Long id,
            @Valid @RequestBody UpdateQuantityRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        cartService.updateQuantity(userId, id, request.getQuantity());
        return Result.success("更新成功", null);
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public Result<Void> deleteItem(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        cartService.deleteItem(userId, id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping("/clear")
    public Result<Void> clearCart() {
        Long userId = securityUtils.getCurrentUserId();
        cartService.clearCart(userId);
        return Result.success("清空成功", null);
    }

    @Operation(summary = "批量选中/取消选中")
    @PutMapping("/select")
    public Result<Void> selectItems(@Valid @RequestBody SelectItemsRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        cartService.selectItems(userId, request);
        return Result.success("操作成功", null);
    }
}
