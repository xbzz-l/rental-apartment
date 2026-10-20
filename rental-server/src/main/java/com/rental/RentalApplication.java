package com.rental;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 公寓租房系统Demo - 后端启动类
 */
@SpringBootApplication
@MapperScan("com.rental.mapper")
public class RentalApplication {

    public static void main(String[] args) {
        SpringApplication.run(RentalApplication.class, args);
    }
}
