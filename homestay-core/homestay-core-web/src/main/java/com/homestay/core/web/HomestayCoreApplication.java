package com.homestay.core.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = "com.homestay.core")
@MapperScan(basePackages = "com.homestay.core.service.mapper")
@EnableFeignClients(basePackages = "com.homestay.core")
@EnableAsync
@EnableScheduling
public class HomestayCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(HomestayCoreApplication.class, args);
        System.out.println("========================================================");
        System.out.println("  多渠道房态管理中台 homestay-core 启动成功!");
        System.out.println("  API: http://localhost:8080");
        System.out.println("========================================================");
    }
}
