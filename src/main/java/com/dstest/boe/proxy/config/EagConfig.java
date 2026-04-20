package com.dstest.boe.proxy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "eag")
public class EagConfig {

    private String baseUrl;
    private Timeout timeout = new Timeout();
    private Mtls mtls = new Mtls();

    @Data
    public static class Timeout {
        private Duration connect = Duration.ofSeconds(10);
        private Duration read = Duration.ofSeconds(60);
    }

    @Data
    public static class Mtls {
        private String certPath;
        private String keyPath;
        private boolean enabled = false;
    }
}
