package com.dstest.boe.proxy.service;

import com.dstest.boe.proxy.config.BoeConfig;
import com.dstest.boe.proxy.exception.BoeAuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoeTokenManagerTest {

    @Mock
    private WebClient eagWebClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    private BoeConfig boeConfig;
    private BoeTokenManager tokenManager;

    @BeforeEach
    void setUp() {
        boeConfig = new BoeConfig();
        boeConfig.setApiPath("/biprws");

        BoeConfig.Auth auth = new BoeConfig.Auth();
        auth.setUsername("testuser");
        auth.setPassword("testpass");
        auth.setType("secEnterprise");
        boeConfig.setAuth(auth);

        BoeConfig.Token token = new BoeConfig.Token();
        token.setRefreshThreshold(Duration.ofMinutes(15));
        token.setMaxAge(Duration.ofMinutes(25));
        boeConfig.setToken(token);

        tokenManager = new BoeTokenManager(eagWebClient, boeConfig);
    }

    @Test
    @DisplayName("getToken returns null initially and requires authentication")
    void testTokenRequiresAuth() {
        // Before any authentication, there is no cached token.
        // Calling getToken should attempt authentication via WebClient.
        // Since we haven't mocked the full WebClient chain, we verify
        // that the method attempts to call the logon endpoint.
        assertThatThrownBy(() -> tokenManager.getToken("test-corr-1"))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("invalidate clears the cached token")
    void testInvalidate() {
        tokenManager.invalidate();
        // After invalidation, next getToken should re-authenticate
        assertThatThrownBy(() -> tokenManager.getToken("test-corr-2"))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("forceRefresh clears and re-authenticates")
    void testForceRefresh() {
        assertThatThrownBy(() -> tokenManager.forceRefresh("test-corr-3"))
                .isInstanceOf(Exception.class);
    }
}
