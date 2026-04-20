package com.dstest.boe.proxy.exception;

public class BoeServiceUnavailableException extends BoeProxyException {

    public BoeServiceUnavailableException(String correlationId) {
        super("BOE_UNAVAILABLE", "The reporting service is temporarily unavailable", correlationId);
    }

    public BoeServiceUnavailableException(String message, String correlationId) {
        super("BOE_UNAVAILABLE", message, correlationId);
    }
}
