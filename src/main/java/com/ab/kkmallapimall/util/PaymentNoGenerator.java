package com.ab.kkmallapimall.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 支付流水号生成工具类
 */
public class PaymentNoGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    /**
     * 生成支付流水号
     * 格式: PAY + 时间戳(14位) + 序列号(4位)
     * 例如: PAY202603180930451234
     */
    public static String generate() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int seq = SEQUENCE.incrementAndGet() % 10000;
        return String.format("PAY%s%04d", timestamp, seq);
    }
}
