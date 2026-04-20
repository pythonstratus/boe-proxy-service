package com.dstest.boe.proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BoeProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoeProxyApplication.class, args);
    }
}
