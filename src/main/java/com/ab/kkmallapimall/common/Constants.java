package com.ab.kkmallapimall.common;

/**
 * 常量类
 */
public class Constants {

    private Constants() {
    }

    /**
     * JWT Token前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * JWT Token请求头
     */
    public static final String TOKEN_HEADER = "Authorization";

    /**
     * Cache constants.
     */
    public static class Cache {
        public static final String PRODUCT_LIST = "mall:product:list";
        public static final String PRODUCT_HOT = "mall:product:hot";

        private Cache() {
        }
    }

    /**
     * 订单状态
     */
    public static class OrderStatus {
        public static final int PENDING_PAYMENT = 0;  // 待付款
        public static final int PENDING_SHIPMENT = 1; // 待发货
        public static final int PENDING_RECEIPT = 2;  // 待收货
        public static final int COMPLETED = 3;        // 已完成
        public static final int CANCELLED = 4;        // 已取消
    }

    /**
     * 用户状态
     */
    public static class UserStatus {
        public static final int DISABLED = 0;  // 禁用
        public static final int ENABLED = 1;   // 启用
    }

    /**
     * 性别
     */
    public static class Gender {
        public static final int UNKNOWN = 0;  // 未知
        public static final int MALE = 1;     // 男
        public static final int FEMALE = 2;   // 女
    }

    /**
     * 逻辑删除
     */
    public static class Deleted {
        public static final int NO = 0;   // 未删除
        public static final int YES = 1;  // 已删除
    }

    /**
     * 是否默认
     */
    public static class IsDefault {
        public static final int NO = 0;   // 否
        public static final int YES = 1;  // 是
    }

    /**
     * 购物车选中状态
     */
    public static class CartSelected {
        public static final int NO = 0;   // 未选中
        public static final int YES = 1;  // 选中
    }
}
