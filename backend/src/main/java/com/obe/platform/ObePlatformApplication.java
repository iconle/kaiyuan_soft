package com.obe.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.obe.platform.**.mapper")
public class ObePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(ObePlatformApplication.class, args);
    }
}
