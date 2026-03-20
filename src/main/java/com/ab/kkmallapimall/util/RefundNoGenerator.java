package com.ab.kkmallapimall.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 退款流水号生成工具类
 */
public class RefundNoGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    /**
     * 生成退款流水号
     * 格式: REF + 时间戳(14位) + 序列号(4位)
     * 例如: REF202603180930451234
     */
    public static String generate() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int seq = SEQUENCE.incrementAndGet() % 10000;
        return String.format("REF%s%04d", timestamp, seq);
    }
}
