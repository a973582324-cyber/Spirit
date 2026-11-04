package com.admaterial;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@MapperScan("com.admaterial.mapper")
public class AdMaterialApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdMaterialApplication.class, args);
    }

    @Bean
    public CommandLineRunner banner() {
        return args -> {
            System.out.println();
            System.out.println("╔══════════════════════════════════════════════════╗");
            System.out.println("║       广告制作公司宣传物料订单管理系统           ║");
            System.out.println("║       AdMaterial Order Management System          ║");
            System.out.println("║                                                   ║");
            System.out.println("║       服务已启动：http://localhost:8080/api      ║");
            System.out.println("╚══════════════════════════════════════════════════╝");
            System.out.println();
        };
    }
}
