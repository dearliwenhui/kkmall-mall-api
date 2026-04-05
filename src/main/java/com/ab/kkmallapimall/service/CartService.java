package com.ab.kkmallapimall.service;

import com.ab.kkmallapimall.common.Constants;
import com.ab.kkmallapimall.dto.request.AddCartRequest;
import com.ab.kkmallapimall.dto.request.SelectItemsRequest;
import com.ab.kkmallapimall.dto.response.CartVO;
import com.ab.kkmallapimall.entity.Cart;
import com.ab.kkmallapimall.entity.Product;
import com.ab.kkmallapimall.exception.BusinessException;
import com.ab.kkmallapimall.exception.ErrorCode;
import com.ab.kkmallapimall.mapper.CartMapper;
import com.ab.kkmallapimall.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 购物车服务
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartMapper cartMapper;
    private final ProductMapper productMapper;

    /**
     * 添加到购物车
     */
    @Transactional(rollbackFor = Exception.class)
    public void addToCart(Long userId, AddCartRequest request) {
        // 检查商品是否存在且上架
        Product product = productMapper.selectById(request.getProductId());
        if (product == null || product.getStatus() == 0) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 检查库存
        if (product.getStock() < request.getQuantity()) {
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_ENOUGH);
        }

        // 检查购物车中是否已存在该商品
        Cart existingCart = cartMapper.selectOne(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, userId)
                        .eq(Cart::getProductId, request.getProductId())
        );

        if (existingCart != null) {
            // 已存在，累加数量
            int newQuantity = existingCart.getQuantity() + request.getQuantity();
            if (product.getStock() < newQuantity) {
                throw new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_ENOUGH);
            }
            existingCart.setQuantity(newQuantity);
            int affected = cartMapper.updateById(existingCart);
            ensureUpdated(affected, "数据已变化，请刷新后重试");
        } else {
            // 不存在，新增
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(request.getProductId());
            cart.setQuantity(request.getQuantity());
            cart.setSelected(Constants.CartSelected.YES);
            cartMapper.insert(cart);
        }
    }

    /**
     * 获取购物车列表
     */
    public List<CartVO> getCartList(Long userId) {
        List<Cart> carts = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, userId)
                        .orderByDesc(Cart::getCreateTime)
        );

        return carts.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 更新数量
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateQuantity(Long userId, Long cartId, Integer quantity) {
        Cart cart = cartMapper.selectById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        // 检查库存
        Product product = productMapper.selectById(cart.getProductId());
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (product.getStock() < quantity) {
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_ENOUGH);
        }

        cart.setQuantity(quantity);
        int affected = cartMapper.updateById(cart);
        ensureUpdated(affected, "数据已变化，请刷新后重试");
    }

    /**
     * 删除商品
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(Long userId, Long cartId) {
        Cart cart = cartMapper.selectById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        cartMapper.deleteById(cartId);
    }

    /**
     * 清空购物车
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearCart(Long userId) {
        cartMapper.delete(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, userId)
        );
    }

    /**
     * 批量选中/取消选中
     */
    @Transactional(rollbackFor = Exception.class)
    public void selectItems(Long userId, SelectItemsRequest request) {
        int selected = Boolean.TRUE.equals(request.getSelected()) ?
                Constants.CartSelected.YES : Constants.CartSelected.NO;

        cartMapper.update(null,
                new LambdaUpdateWrapper<Cart>()
                        .set(Cart::getSelected, selected)
                        .eq(Cart::getUserId, userId)
                        .in(Cart::getId, request.getIds())
        );
    }

    /**
     * 转换为VO
     */
    private CartVO convertToVO(Cart cart) {
        CartVO vo = new CartVO();
        BeanUtils.copyProperties(cart, vo);

        // 查询商品信息
        Product product = productMapper.selectById(cart.getProductId());
        if (product != null) {
            vo.setProductName(product.getProductName());
            vo.setPrice(product.getPrice());
            vo.setStock(product.getStock());

            // 获取主图
            if (StringUtils.hasText(product.getImages())) {
                String[] images = product.getImages().split(",");
                vo.setProductImage(images.length > 0 ? images[0] : null);
            }

            // 计算小计
            vo.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
        }

        return vo;
    }

    private void ensureUpdated(int affected, String message) {
        if (affected > 0) {
            return;
        }
        throw new BusinessException(409, message);
    }
}
