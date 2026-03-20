package com.ab.kkmallapimall.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 订单号生成器
 */
public class OrderNoGenerator {

    private static final AtomicLong SEQUENCE = new AtomicLong(0);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 生成订单号
     * 格式：时间戳(14位) + 序列号(6位)
     */
    public static String generate() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        long sequence = SEQUENCE.incrementAndGet() % 1000000;
        return timestamp + String.format("%06d", sequence);
    }
}
