package com.ab.kkmallapimall.common;

/**
 * Constants.
 */
public class Constants {

    private Constants() {
    }

    /**
     * JWT token prefix.
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * JWT token header.
     */
    public static final String TOKEN_HEADER = "Authorization";

    /**
     * Internal API token header.
     */
    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    public static class Cache {
        public static final String PRODUCT_LIST = "mall:product:list";
        public static final String PRODUCT_HOT = "mall:product:hot";

        private Cache() {
        }
    }

    public static class OrderStatus {
        public static final int PENDING_PAYMENT = 0;
        public static final int PENDING_SHIPMENT = 1;
        public static final int PENDING_RECEIPT = 2;
        public static final int COMPLETED = 3;
        public static final int CANCELLED = 4;
        public static final int CLOSED = 5;

        private OrderStatus() {
        }
    }

    public static class UserStatus {
        public static final int DISABLED = 0;
        public static final int ENABLED = 1;

        private UserStatus() {
        }
    }

    public static class Gender {
        public static final int UNKNOWN = 0;
        public static final int MALE = 1;
        public static final int FEMALE = 2;

        private Gender() {
        }
    }

    public static class Deleted {
        public static final int NO = 0;
        public static final int YES = 1;

        private Deleted() {
        }
    }

    public static class IsDefault {
        public static final int NO = 0;
        public static final int YES = 1;

        private IsDefault() {
        }
    }

    public static class CartSelected {
        public static final int NO = 0;
        public static final int YES = 1;

        private CartSelected() {
        }
    }
}
