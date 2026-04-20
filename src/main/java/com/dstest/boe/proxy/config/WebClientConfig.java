package com.dstest.boe.proxy.config;

import io.netty.handler.ssl.SslContextBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.io.File;
import java.time.Duration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final EagConfig eagConfig;

    @Bean
    public WebClient eagWebClient(WebClient.Builder builder) {
        HttpClient httpClient = buildHttpClient();

        return builder
                .baseUrl(eagConfig.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    private HttpClient buildHttpClient() {
        HttpClient client = HttpClient.create()
                .responseTimeout(eagConfig.getTimeout().getRead());

        if (eagConfig.getMtls().isEnabled()) {
            client = client.secure(sslSpec -> {
                try {
                    sslSpec.sslContext(SslContextBuilder.forClient()
                            .keyManager(
                                    new File(eagConfig.getMtls().getCertPath()),
                                    new File(eagConfig.getMtls().getKeyPath())
                            )
                            .build());
                } catch (Exception e) {
                    throw new RuntimeException("Failed to configure mTLS for EAG WebClient", e);
                }
            });
        }

        return client;
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.debug("EAG Request: {} {}", request.method(), request.url());
            return Mono.just(request);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.debug("EAG Response: {} from {}", response.statusCode(), response.headers().asHttpHeaders().getFirst("X-Request-Id"));
            return Mono.just(response);
        });
    }
}
