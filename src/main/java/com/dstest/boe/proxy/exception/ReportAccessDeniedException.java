package com.dstest.boe.proxy.exception;

public class ReportAccessDeniedException extends BoeProxyException {

    public ReportAccessDeniedException(String correlationId) {
        super("REPORT_ACCESS_DENIED", "You do not have permission to view this report", correlationId);
    }
}
