package com.dstest.boe.proxy.exception;

public class ReportNotFoundException extends BoeProxyException {

    public ReportNotFoundException(String reportCode, String correlationId) {
        super("REPORT_NOT_FOUND", "Report code not found in registry: " + reportCode, correlationId);
    }
}
