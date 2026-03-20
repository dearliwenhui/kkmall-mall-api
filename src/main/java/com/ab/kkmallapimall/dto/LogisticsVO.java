package com.ab.kkmallapimall.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 物流信息视图对象
 */
@Data
public class LogisticsVO {

    private String logisticsCompany;

    private String trackingNumber;

    private LocalDateTime shipTime;

    private String status;

    private List<LogisticsTrace> traces;

    @Data
    public static class LogisticsTrace {
        private String time;
        private String status;
        private String description;
    }
}
