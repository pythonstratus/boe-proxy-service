package com.dstest.boe.proxy.service;

import com.dstest.boe.proxy.config.ProxyConfig;
import com.dstest.boe.proxy.exception.BoeServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

class CircuitBreakerServiceTest {

    private CircuitBreakerService circuitBreaker;

    @BeforeEach
    void setUp() {
        ProxyConfig config = new ProxyConfig();
        ProxyConfig.CircuitBreakerConfig cbConfig = new ProxyConfig.CircuitBreakerConfig();
        cbConfig.setFailureThreshold(3);
        cbConfig.setResetInterval(Duration.ofSeconds(1));
        cbConfig.setFailureWindow(Duration.ofSeconds(60));
        config.setCircuitBreaker(cbConfig);

        circuitBreaker = new CircuitBreakerService(config);
    }

    @Test
    @DisplayName("starts in CLOSED state")
    void testInitialStateClosed() {
        assertThat(circuitBreaker.getState())
                .isEqualTo(CircuitBreakerService.State.CLOSED);
    }

    @Test
    @DisplayName("checkState allows requests when CLOSED")
    void testClosedAllowsRequests() {
        assertThatCode(() -> circuitBreaker.checkState("test-corr"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("opens after reaching failure threshold")
    void testOpensAfterThreshold() {
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();

        assertThat(circuitBreaker.getState())
                .isEqualTo(CircuitBreakerService.State.OPEN);
    }

    @Test
    @DisplayName("rejects requests when OPEN")
    void testOpenRejectsRequests() {
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();

        assertThatThrownBy(() -> circuitBreaker.checkState("test-corr"))
                .isInstanceOf(BoeServiceUnavailableException.class);
    }

    @Test
    @DisplayName("success resets failure count and closes circuit")
    void testSuccessResetsCount() {
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        circuitBreaker.recordSuccess();

        assertThat(circuitBreaker.getState())
                .isEqualTo(CircuitBreakerService.State.CLOSED);

        // Should be able to tolerate more failures now
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        assertThat(circuitBreaker.getState())
                .isEqualTo(CircuitBreakerService.State.CLOSED);
    }

    @Test
    @DisplayName("transitions to HALF_OPEN after reset interval")
    void testTransitionsToHalfOpen() throws InterruptedException {
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();

        assertThat(circuitBreaker.getState())
                .isEqualTo(CircuitBreakerService.State.OPEN);

        // Wait for reset interval (1 second in test config)
        Thread.sleep(1200);

        // checkState should transition to HALF_OPEN and allow the request
        assertThatCode(() -> circuitBreaker.checkState("test-corr"))
                .doesNotThrowAnyException();

        assertThat(circuitBreaker.getState())
                .isEqualTo(CircuitBreakerService.State.HALF_OPEN);
    }

    @Test
    @DisplayName("HALF_OPEN closes on success")
    void testHalfOpenClosesOnSuccess() throws InterruptedException {
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();

        Thread.sleep(1200);
        circuitBreaker.checkState("test-corr");

        assertThat(circuitBreaker.getState())
                .isEqualTo(CircuitBreakerService.State.HALF_OPEN);

        circuitBreaker.recordSuccess();

        assertThat(circuitBreaker.getState())
                .isEqualTo(CircuitBreakerService.State.CLOSED);
    }

    @Test
    @DisplayName("HALF_OPEN re-opens on failure")
    void testHalfOpenReopensOnFailure() throws InterruptedException {
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();

        Thread.sleep(1200);
        circuitBreaker.checkState("test-corr");

        assertThat(circuitBreaker.getState())
                .isEqualTo(CircuitBreakerService.State.HALF_OPEN);

        circuitBreaker.recordFailure();

        assertThat(circuitBreaker.getState())
                .isEqualTo(CircuitBreakerService.State.OPEN);
    }
}
