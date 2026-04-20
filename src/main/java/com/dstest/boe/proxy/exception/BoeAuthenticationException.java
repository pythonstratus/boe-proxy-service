package com.dstest.boe.proxy.exception;

public class BoeAuthenticationException extends BoeProxyException {

    public BoeAuthenticationException(String message, String correlationId) {
        super("BOE_AUTH_FAILED", message, correlationId);
    }

    public BoeAuthenticationException(String message, String correlationId, Throwable cause) {
        super("BOE_AUTH_FAILED", message, correlationId, cause);
    }
}
