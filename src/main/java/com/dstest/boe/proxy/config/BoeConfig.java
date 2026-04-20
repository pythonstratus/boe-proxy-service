package com.dstest.boe.proxy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "boe")
public class BoeConfig {

    private String apiPath = "/biprws";
    private String raylightPath = "/biprws/raylight/v1";
    private Auth auth = new Auth();
    private Token token = new Token();

    @Data
    public static class Auth {
        private String username;
        private String password;
        private String type = "secEnterprise";
    }

    @Data
    public static class Token {
        private Duration refreshThreshold = Duration.ofMinutes(15);
        private Duration maxAge = Duration.ofMinutes(25);
    }
}
