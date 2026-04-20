package com.dstest.boe.proxy.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ErrorResponse {

    private String error;
    private String message;
    private String correlationId;

    @Builder.Default
    private String timestamp = Instant.now().toString();
}
