package com.fehead.open.common;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @Description:
 * @Author lmwis
 * @Date 2019-11-11 18:00
 * @Version 1.0
 */
@SpringBootApplication(scanBasePackages={"com.fehead.lang","com.fehead.open.common"})
//@EnableEurekaClient
@Configuration
@EnableFeignClients // 开启feign支持
@EnableDiscoveryClient // nacos
public class FeheadCommonServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FeheadCommonServerApplication.class,args);
    }


    @Bean
    public JavaMailSender javaMailSender(){
        return new JavaMailSenderImpl();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
