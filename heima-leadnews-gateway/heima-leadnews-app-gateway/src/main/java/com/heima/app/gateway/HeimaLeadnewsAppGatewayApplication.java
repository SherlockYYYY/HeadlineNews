package com.heima.app.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient //开启注册中心 让nacos发现自己
public class HeimaLeadnewsAppGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeimaLeadnewsAppGatewayApplication.class, args);
    }

}
