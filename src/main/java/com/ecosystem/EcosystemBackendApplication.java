package com.ecosystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // 启用定时任务（用于清理过期session）
public class EcosystemBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcosystemBackendApplication.class, args);
    }
}

