package com.healthapp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.healthapp.mapper")
public class HealthAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(HealthAppApplication.class, args);
    }
}
