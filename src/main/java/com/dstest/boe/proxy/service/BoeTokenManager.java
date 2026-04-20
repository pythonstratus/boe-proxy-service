package com.dstest.boe.proxy.service;

import com.dstest.boe.proxy.config.BoeConfig;
import com.dstest.boe.proxy.exception.BoeAuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages BOE session token lifecycle. Singleton — all report requests share one token.
 *
 * <p>Authenticates via POST /biprws/logon/long through EAG. Caches the X-SAP-LogonToken
 * in memory and proactively refreshes before expiry. Thread-safe via ReentrantLock
 * to prevent multiple simultaneous re-auth calls.</p>
 *
 * <p>Usage: call {@link #getToken(String)} before every Raylight request.
 * The method returns a cached token or transparently re-authenticates.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BoeTokenManager {

    private final WebClient eagWebClient;
    private final BoeConfig boeConfig;

    private final ReentrantLock refreshLock = new ReentrantLock();

    private volatile String cachedToken;
    private volatile Instant tokenIssuedAt;

    /**
     * Returns a valid BOE logon token. Authenticates if no token exists
     * or if the current token has exceeded the refresh threshold.
     *
     * @param correlationId for tracing across the request chain
     * @return the X-SAP-LogonToken value
     * @throws BoeAuthenticationException if authentication fails
     */
    public String getToken(String correlationId) {
        if (isTokenValid()) {
            return cachedToken;
        }
        return refreshToken(correlationId);
    }

    /**
     * Forces a token refresh. Called when a Raylight call returns 401
     * despite the token being within its TTL (e.g., BOE restarted).
     *
     * @param correlationId for tracing
     * @return the new X-SAP-LogonToken value
     */
    public String forceRefresh(String correlationId) {
        log.info("Forcing token refresh [correlationId={}]", correlationId);
        cachedToken = null;
        tokenIssuedAt = null;
        return refreshToken(correlationId);
    }

    /**
     * Invalidates the current token without re-authenticating.
     */
    public void invalidate() {
        cachedToken = null;
        tokenIssuedAt = null;
        log.info("BOE token invalidated");
    }

    private boolean isTokenValid() {
        if (cachedToken == null || tokenIssuedAt == null) {
            return false;
        }
        Instant refreshAt = tokenIssuedAt.plus(boeConfig.getToken().getRefreshThreshold());
        return Instant.now().isBefore(refreshAt);
    }

    private String refreshToken(String correlationId) {
        refreshLock.lock();
        try {
            // Double-check after acquiring lock — another thread may have refreshed already
            if (isTokenValid()) {
                return cachedToken;
            }

            log.info("Authenticating with BOE via EAG [correlationId={}]", correlationId);

            Map<String, String> loginPayload = Map.of(
                    "userName", boeConfig.getAuth().getUsername(),
                    "password", boeConfig.getAuth().getPassword(),
                    "auth", boeConfig.getAuth().getType()
            );

            String token = eagWebClient.post()
                    .uri(boeConfig.getApiPath() + "/logon/long")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Correlation-Id", correlationId)
                    .bodyValue(loginPayload)
                    .exchangeToMono(response -> {
                        if (response.statusCode().is2xxSuccessful()) {
                            String logonToken = response.headers()
                                    .header("X-SAP-LogonToken")
                                    .stream()
                                    .findFirst()
                                    .orElse(null);
                            return response.releaseBody().thenReturn(logonToken != null ? logonToken : "");
                        } else {
                            return response.createException().flatMap(reactor.core.publisher.Mono::error);
                        }
                    })
                    .block();

            if (token == null || token.isBlank()) {
                throw new BoeAuthenticationException(
                        "BOE logon succeeded but no X-SAP-LogonToken header in response",
                        correlationId
                );
            }

            cachedToken = token;
            tokenIssuedAt = Instant.now();
            log.info("BOE token acquired successfully [correlationId={}]", correlationId);
            return cachedToken;

        } catch (WebClientResponseException e) {
            log.error("BOE authentication failed: {} {} [correlationId={}]",
                    e.getStatusCode(), e.getStatusText(), correlationId);
            throw new BoeAuthenticationException(
                    "BOE authentication failed: " + e.getStatusCode(),
                    correlationId, e
            );
        } catch (BoeAuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("BOE authentication error [correlationId={}]", correlationId, e);
            throw new BoeAuthenticationException(
                    "Failed to authenticate with BOE: " + e.getMessage(),
                    correlationId, e
            );
        } finally {
            refreshLock.unlock();
        }
    }
}
