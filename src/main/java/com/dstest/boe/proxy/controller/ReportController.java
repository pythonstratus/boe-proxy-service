package com.dstest.boe.proxy.controller;

import com.dstest.boe.proxy.dto.request.ReportRunRequest;
import com.dstest.boe.proxy.dto.response.*;
import com.dstest.boe.proxy.exception.JobNotFoundException;
import com.dstest.boe.proxy.exception.BoeProxyException;
import com.dstest.boe.proxy.model.ReportDefinition;
import com.dstest.boe.proxy.model.ReportJob;
import com.dstest.boe.proxy.model.ReportStatus;
import com.dstest.boe.proxy.service.ReportExecutor;
import com.dstest.boe.proxy.service.ReportRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller exposing the proxy's API to the Entity frontend.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /api/reports/run} — submit a report for execution</li>
 *   <li>{@code GET /api/reports/{jobId}/status} — poll job status</li>
 *   <li>{@code GET /api/reports/{jobId}/results} — retrieve completed results</li>
 *   <li>{@code DELETE /api/reports/{jobId}} — cancel a running job</li>
 * </ul>
 *
 * <p>The frontend never interacts with BOE or EAG directly. All BOE complexity
 * is encapsulated behind these four endpoints.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportRegistry reportRegistry;
    private final ReportExecutor reportExecutor;

    /**
     * Submit a report for execution.
     *
     * <p>The proxy resolves the report code to a BOE Document ID, validates
     * required parameters, and delegates to the executor which decides
     * sync vs async based on the report's expected runtime tier.</p>
     *
     * @param request contains reportCode and parameters
     * @return job ID and initial status (COMPLETE for fast sync, SUBMITTED for async)
     */
    @PostMapping("/run")
    public ResponseEntity<ReportSubmitResponse> runReport(
            @Valid @RequestBody ReportRunRequest request,
            @RequestHeader(value = "X-Entity-User-Id", required = false) String userId) {

        String correlationId = generateCorrelationId();
        log.info("Report run request: code={}, correlationId={}, user={}",
                request.getReportCode(), correlationId, userId);

        // Resolve report code to BOE definition
        ReportDefinition definition = reportRegistry.lookup(
                request.getReportCode(), correlationId);

        // Validate required parameters
        reportRegistry.validateParameters(
                definition, request.getParameters(), correlationId);

        // Execute (sync for FAST, async for SLOW)
        ReportJob job = reportExecutor.executeReport(
                definition, request.getParameters(), correlationId, userId);

        HttpStatus status = job.getStatus() == ReportStatus.COMPLETE
                ? HttpStatus.OK
                : HttpStatus.ACCEPTED;

        return ResponseEntity.status(status)
                .body(ReportSubmitResponse.builder()
                        .jobId(job.getJobId())
                        .status(job.getStatus())
                        .reportCode(request.getReportCode())
                        .message(job.getStatus() == ReportStatus.COMPLETE
                                ? "Report completed"
                                : "Report submitted for processing")
                        .build());
    }

    /**
     * Poll the status of a submitted report job.
     *
     * <p>This is a fast, local read from the in-memory job tracker.
     * It does NOT trigger a BOE call — the polling engine handles that
     * in the background.</p>
     *
     * @param jobId the proxy-generated job ID from the run response
     * @return current status, elapsed time, and poll count
     */
    @GetMapping("/{jobId}/status")
    public ResponseEntity<ReportStatusResponse> getStatus(@PathVariable String jobId) {
        ReportJob job = reportExecutor.getJob(jobId);
        if (job == null) {
            throw new JobNotFoundException(jobId, "N/A");
        }

        return ResponseEntity.ok(ReportStatusResponse.builder()
                .jobId(job.getJobId())
                .status(job.getStatus())
                .elapsedSeconds(job.getElapsedSeconds())
                .pollCount(job.getPollCount())
                .message(buildStatusMessage(job))
                .build());
    }

    /**
     * Retrieve the results of a completed report job.
     *
     * <p>Returns 404 if the job doesn't exist, 409 if the job hasn't
     * completed yet, and 200 with structured data if complete.</p>
     *
     * @param jobId the proxy-generated job ID
     * @return columns, rows, and metadata
     */
    @GetMapping("/{jobId}/results")
    public ResponseEntity<ReportResultsResponse> getResults(@PathVariable String jobId) {
        ReportJob job = reportExecutor.getJob(jobId);
        if (job == null) {
            throw new JobNotFoundException(jobId, "N/A");
        }

        if (job.getStatus() != ReportStatus.COMPLETE) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ReportResultsResponse.builder()
                            .jobId(jobId)
                            .build());
        }

        return ResponseEntity.ok(ReportResultsResponse.builder()
                .jobId(job.getJobId())
                .columns(job.getCachedResults().getColumns())
                .rows(job.getCachedResults().getRows())
                .metadata(job.getCachedResults().getMetadata())
                .build());
    }

    /**
     * Cancel a running report job.
     *
     * <p>Stops the polling engine from checking this job and marks it
     * as cancelled. Optionally attempts to cancel the BOE job.</p>
     *
     * @param jobId the proxy-generated job ID
     * @return cancellation confirmation
     */
    @DeleteMapping("/{jobId}")
    public ResponseEntity<ReportCancelResponse> cancelReport(@PathVariable String jobId) {
        ReportJob job = reportExecutor.getJob(jobId);
        if (job == null) {
            throw new JobNotFoundException(jobId, "N/A");
        }

        boolean cancelled = reportExecutor.cancelJob(jobId);

        return ResponseEntity.ok(ReportCancelResponse.builder()
                .jobId(jobId)
                .cancelled(cancelled)
                .message(cancelled ? "Report cancelled" : "Unable to cancel report")
                .build());
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private String generateCorrelationId() {
        return "RPT-" + UUID.randomUUID().toString().substring(0, 12);
    }

    private String buildStatusMessage(ReportJob job) {
        return switch (job.getStatus()) {
            case SUBMITTED -> "Report submitted, waiting to start";
            case RUNNING -> "Report is running (" + job.getElapsedSeconds() + "s elapsed)";
            case COMPLETE -> "Report completed";
            case FAILED -> "Report failed: " + (job.getErrorMessage() != null
                    ? job.getErrorMessage() : "Unknown error");
            case CANCELLED -> "Report was cancelled";
            case TIMED_OUT -> "Report execution timed out";
        };
    }
}
