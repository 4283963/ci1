package com.homestay.sync;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.homestay.sync.client")
@EnableAsync
@EnableScheduling
@MapperScan("com.homestay.sync.mapper")
public class HomestaySyncApplication {
    public static void main(String[] args) {
        SpringApplication.run(HomestaySyncApplication.class, args);
        System.out.println("========================================================");
        System.out.println("  多渠道房态同步微服务 homestay-sync 启动成功!");
        System.out.println("  Webhook接入: http://localhost:8081/webhook/*");
        System.out.println("========================================================");
    }
}
