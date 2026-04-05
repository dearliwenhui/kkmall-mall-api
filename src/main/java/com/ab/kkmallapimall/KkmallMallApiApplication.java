package com.ab.kkmallapimall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * KKMall商城后端API服务
 */
@SpringBootApplication
@MapperScan("com.ab.kkmallapimall.mapper")
@EnableCaching
@EnableScheduling
public class KkmallMallApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(KkmallMallApiApplication.class, args);
    }
}
