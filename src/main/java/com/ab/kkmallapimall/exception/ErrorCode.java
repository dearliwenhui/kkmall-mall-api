package com.ab.kkmallapimall.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 通用错误
    SYSTEM_ERROR(500, "系统错误"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),

    // 用户相关
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    USERNAME_OR_PASSWORD_ERROR(1003, "用户名或密码错误"),
    USER_DISABLED(1004, "用户已被禁用"),
    PHONE_ALREADY_EXISTS(1005, "手机号已存在"),
    OLD_PASSWORD_ERROR(1006, "原密码错误"),

    // 商品相关
    PRODUCT_NOT_FOUND(2001, "商品不存在"),
    PRODUCT_STOCK_NOT_ENOUGH(2002, "商品库存不足"),
    CATEGORY_NOT_FOUND(2003, "分类不存在"),

    // 购物车相关
    CART_ITEM_NOT_FOUND(3001, "购物车商品不存在"),
    CART_EMPTY(3002, "购物车为空"),

    // 订单相关
    ORDER_NOT_FOUND(4001, "订单不存在"),
    ORDER_STATUS_ERROR(4002, "订单状态错误"),
    ORDER_CANNOT_CANCEL(4003, "订单无法取消"),
    ORDER_CANNOT_CONFIRM(4004, "订单无法确认收货"),

    // 地址相关
    ADDRESS_NOT_FOUND(5001, "地址不存在"),
    DEFAULT_ADDRESS_NOT_FOUND(5002, "默认地址不存在"),

    // 支付相关
    PAYMENT_NOT_FOUND(6001, "支付记录不存在"),
    PAYMENT_STATUS_ERROR(6002, "支付状态错误"),
    PAYMENT_AMOUNT_ERROR(6003, "支付金额错误"),

    // 评价相关
    REVIEW_NOT_FOUND(7001, "评价不存在"),
    REVIEW_ALREADY_EXISTS(7002, "已评价过该商品"),

    // 退款相关
    REFUND_NOT_FOUND(8001, "退款申请不存在"),
    REFUND_STATUS_ERROR(8002, "退款状态错误"),

    // 优惠券相关
    COUPON_NOT_FOUND(9001, "优惠券不存在"),
    COUPON_NOT_AVAILABLE(9002, "优惠券不可用"),
    COUPON_ALREADY_RECEIVED(9003, "优惠券已领取"),
    COUPON_STOCK_NOT_ENOUGH(9004, "优惠券库存不足"),

    // 收藏相关
    FAVORITE_ALREADY_EXISTS(10001, "已收藏该商品");

    private final Integer code;
    private final String message;
}
