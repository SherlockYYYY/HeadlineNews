package com.heima.app.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDiscoveryClient //开启注册中心 让nacos发现自己
public class  AdminGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run( AdminGatewayApplication.class, args);
    }

}
