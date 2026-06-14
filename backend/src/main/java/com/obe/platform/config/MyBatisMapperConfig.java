package com.obe.platform.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.obe.platform.**.mapper")
public class MyBatisMapperConfig {
}
