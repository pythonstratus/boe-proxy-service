package com.dstest.boe.proxy.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class ReportJob {

    /**
     * Proxy-generated job ID returned to the frontend.
     */
    private String jobId;

    /**
     * BOE schedule ID / instance ID returned by Raylight after submission.
     */
    private String boeScheduleId;

    /**
     * The report definition this job is executing.
     */
    private ReportDefinition reportDefinition;

    /**
     * Parameters passed by the frontend for this report execution.
     */
    private Map<String, String> parameters;

    /**
     * Current status of the job.
     */
    private ReportStatus status;

    /**
     * When the job was submitted.
     */
    private Instant submittedAt;

    /**
     * When the job status was last checked via polling.
     */
    private Instant lastPolledAt;

    /**
     * Number of poll attempts made so far.
     */
    private int pollCount;

    /**
     * When the job completed (success or failure).
     */
    private Instant completedAt;

    /**
     * Cached report results (populated when status is COMPLETE).
     * Stored as raw parsed data before transformation.
     */
    private ReportResults cachedResults;

    /**
     * Error message if the job failed.
     */
    private String errorMessage;

    /**
     * Correlation ID for distributed tracing across Entity → EAG → BOE.
     */
    private String correlationId;

    /**
     * The user identity that initiated this report (for audit/security).
     */
    private String requestedBy;

    public long getElapsedSeconds() {
        Instant end = completedAt != null ? completedAt : Instant.now();
        return java.time.Duration.between(submittedAt, end).getSeconds();
    }
}
