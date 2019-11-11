package com.fehead.open.common;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * @Description:
 * @Author lmwis
 * @Date 2019-11-11 18:00
 * @Version 1.0
 */
@SpringBootApplication
@EnableEurekaClient
public class FeheadCommonServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FeheadCommonServerApplication.class,args);
    }
}
