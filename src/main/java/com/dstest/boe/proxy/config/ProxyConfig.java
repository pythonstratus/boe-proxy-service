package com.dstest.boe.proxy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "proxy")
public class ProxyConfig {

    private Polling polling = new Polling();
    private JobTracker jobTracker = new JobTracker();
    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();

    @Data
    public static class Polling {
        private Duration initialDelay = Duration.ofSeconds(3);
        private Duration fastInterval = Duration.ofSeconds(3);
        private Duration slowInterval = Duration.ofSeconds(10);
        private Duration maxDuration = Duration.ofMinutes(10);
        private int maxAttempts = 60;
    }

    @Data
    public static class JobTracker {
        private Duration cleanupAfter = Duration.ofMinutes(30);
    }

    @Data
    public static class CircuitBreakerConfig {
        private int failureThreshold = 5;
        private Duration resetInterval = Duration.ofSeconds(30);
        private Duration failureWindow = Duration.ofSeconds(60);
    }
}
