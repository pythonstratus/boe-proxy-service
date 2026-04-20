package com.dstest.boe.proxy.exception;

import lombok.Getter;

@Getter
public class BoeProxyException extends RuntimeException {

    private final String errorCode;
    private final String correlationId;

    public BoeProxyException(String errorCode, String message, String correlationId) {
        super(message);
        this.errorCode = errorCode;
        this.correlationId = correlationId;
    }

    public BoeProxyException(String errorCode, String message, String correlationId, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.correlationId = correlationId;
    }
}
