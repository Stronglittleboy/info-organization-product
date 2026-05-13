package com.example.infoorg;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.example.infoorg.mapper")
@SpringBootApplication
public class InfoOrganizationApplication {

    public static void main(String[] args) {
        SpringApplication.run(InfoOrganizationApplication.class, args);
    }
}
