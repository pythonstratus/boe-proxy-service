package com.dstest.boe.proxy.service;

import com.dstest.boe.proxy.config.ProxyConfig;
import com.dstest.boe.proxy.exception.BoeServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simple circuit breaker for BOE calls through EAG.
 *
 * <p>States:
 * <ul>
 *   <li><b>CLOSED</b> — normal operation, requests pass through.</li>
 *   <li><b>OPEN</b> — BOE is consistently failing, requests are rejected immediately.</li>
 *   <li><b>HALF_OPEN</b> — a health check is attempted; success closes, failure re-opens.</li>
 * </ul>
 *
 * <p>The circuit opens when consecutive failures within the configured failure window
 * exceed the threshold. It transitions to HALF_OPEN after the reset interval elapses.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CircuitBreakerService {

    private final ProxyConfig proxyConfig;

    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicReference<Instant> firstFailureAt = new AtomicReference<>(null);
    private final AtomicReference<Instant> circuitOpenedAt = new AtomicReference<>(null);

    private volatile State state = State.CLOSED;

    public enum State {
        CLOSED, OPEN, HALF_OPEN
    }

    /**
     * Checks whether a request should be allowed through.
     * If the circuit is open, throws BoeServiceUnavailableException.
     * If the reset interval has elapsed, transitions to HALF_OPEN and allows one request.
     *
     * @param correlationId for tracing
     */
    public void checkState(String correlationId) {
        if (state == State.CLOSED) {
            return;
        }

        if (state == State.OPEN) {
            Instant openedAt = circuitOpenedAt.get();
            if (openedAt != null && Instant.now().isAfter(
                    openedAt.plus(proxyConfig.getCircuitBreaker().getResetInterval()))) {
                log.info("Circuit breaker transitioning to HALF_OPEN [correlationId={}]", correlationId);
                state = State.HALF_OPEN;
                return; // Allow this request as a health check
            }
            throw new BoeServiceUnavailableException(correlationId);
        }

        // HALF_OPEN — allow the request through (it's the health check probe)
    }

    /**
     * Records a successful BOE call. Resets the failure count and closes the circuit.
     */
    public void recordSuccess() {
        if (state != State.CLOSED) {
            log.info("Circuit breaker CLOSED — BOE recovered");
        }
        consecutiveFailures.set(0);
        firstFailureAt.set(null);
        circuitOpenedAt.set(null);
        state = State.CLOSED;
    }

    /**
     * Records a failed BOE call. Opens the circuit if the threshold is reached.
     */
    public void recordFailure() {
        Instant now = Instant.now();

        // If this is a half-open probe that failed, re-open immediately
        if (state == State.HALF_OPEN) {
            log.warn("Circuit breaker re-OPENED — half-open probe failed");
            circuitOpenedAt.set(now);
            state = State.OPEN;
            return;
        }

        // Track first failure time for windowing
        firstFailureAt.compareAndSet(null, now);

        // Check if failures are within the window
        Instant windowStart = firstFailureAt.get();
        if (windowStart != null && now.isAfter(
                windowStart.plus(proxyConfig.getCircuitBreaker().getFailureWindow()))) {
            // Window expired — reset counter and start fresh
            consecutiveFailures.set(1);
            firstFailureAt.set(now);
            return;
        }

        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= proxyConfig.getCircuitBreaker().getFailureThreshold()) {
            log.error("Circuit breaker OPENED — {} consecutive failures within window", failures);
            circuitOpenedAt.set(now);
            state = State.OPEN;
        }
    }

    /**
     * Returns the current state of the circuit breaker. Useful for actuator/health checks.
     */
    public State getState() {
        return state;
    }
}
