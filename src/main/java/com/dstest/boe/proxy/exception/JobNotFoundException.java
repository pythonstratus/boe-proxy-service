package com.dstest.boe.proxy.exception;

public class JobNotFoundException extends BoeProxyException {

    public JobNotFoundException(String jobId, String correlationId) {
        super("JOB_NOT_FOUND", "No report job found with ID: " + jobId, correlationId);
    }
}
