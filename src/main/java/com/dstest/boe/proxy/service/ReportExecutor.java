package com.dstest.boe.proxy.service;

import com.dstest.boe.proxy.config.BoeConfig;
import com.dstest.boe.proxy.exception.BoeProxyException;
import com.dstest.boe.proxy.exception.ReportAccessDeniedException;
import com.dstest.boe.proxy.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core orchestration service for report execution.
 *
 * <p>Receives run requests from the controller, decides sync vs async based on
 * the report's expected runtime tier, and delegates to the appropriate flow.</p>
 *
 * <p>For FAST reports: executes the full Raylight call sequence synchronously
 * and returns results directly.</p>
 *
 * <p>For SLOW reports: submits the report to BOE's scheduling API, stores the
 * job in the tracker, and returns a job ID immediately. The {@link PollingEngine}
 * handles status checks and result retrieval asynchronously.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportExecutor {

    private final WebClient eagWebClient;
    private final BoeConfig boeConfig;
    private final BoeTokenManager tokenManager;
    private final CircuitBreakerService circuitBreaker;
    private final ResponseTransformer responseTransformer;

    /**
     * In-memory job tracker. Keyed by proxy-generated job ID.
     * Shared with {@link PollingEngine} for status updates and result caching.
     */
    private final Map<String, ReportJob> jobTracker = new ConcurrentHashMap<>();

    /**
     * Executes a report — synchronously for FAST reports, asynchronously for SLOW reports.
     *
     * @param definition    the resolved report definition from the registry
     * @param parameters    user-provided report parameters
     * @param correlationId for distributed tracing
     * @param requestedBy   the user identity that initiated this report
     * @return the report job (status is COMPLETE for sync, SUBMITTED for async)
     */
    public ReportJob executeReport(ReportDefinition definition,
                                   Map<String, String> parameters,
                                   String correlationId,
                                   String requestedBy) {
        circuitBreaker.checkState(correlationId);

        String jobId = generateJobId();
        ReportJob job = ReportJob.builder()
                .jobId(jobId)
                .reportDefinition(definition)
                .parameters(parameters)
                .status(ReportStatus.SUBMITTED)
                .submittedAt(Instant.now())
                .correlationId(correlationId)
                .requestedBy(requestedBy)
                .build();

        jobTracker.put(jobId, job);

        if (definition.getExpectedRuntimeTier() == RuntimeTier.FAST) {
            return executeSynchronous(job);
        } else {
            return executeAsynchronous(job);
        }
    }

    /**
     * Retrieves a job from the tracker by its ID.
     */
    public ReportJob getJob(String jobId) {
        return jobTracker.get(jobId);
    }

    /**
     * Cancels a running job. Stops polling and optionally cancels the BOE job.
     */
    public boolean cancelJob(String jobId) {
        ReportJob job = jobTracker.get(jobId);
        if (job == null) {
            return false;
        }
        job.setStatus(ReportStatus.CANCELLED);
        job.setCompletedAt(Instant.now());
        log.info("Job {} cancelled [correlationId={}]", jobId, job.getCorrelationId());
        return true;
    }

    /**
     * Returns the full job tracker map. Used by the polling engine.
     */
    public Map<String, ReportJob> getJobTracker() {
        return jobTracker;
    }

    // ── Synchronous flow ────────────────────────────────────────────────

    private ReportJob executeSynchronous(ReportJob job) {
        String correlationId = job.getCorrelationId();
        String docId = job.getReportDefinition().getBoeDocumentId();

        try {
            job.setStatus(ReportStatus.RUNNING);
            String token = tokenManager.getToken(correlationId);
            Instant startTime = Instant.now();

            // Step 1: Open document
            log.debug("Opening BOE document {} [correlationId={}]", docId, correlationId);
            raylightGet("/documents/" + docId, token, correlationId);

            // Step 2: Set prompt parameters
            if (job.getParameters() != null && !job.getParameters().isEmpty()) {
                log.debug("Setting parameters for document {} [correlationId={}]", docId, correlationId);
                Map<String, Object> paramPayload = buildParameterPayload(job.getParameters());
                raylightPut("/documents/" + docId + "/parameters", paramPayload, token, correlationId);
            }

            // Step 3: Refresh data providers
            log.debug("Refreshing data providers for document {} [correlationId={}]", docId, correlationId);
            raylightPut("/documents/" + docId + "/dataproviders", null, token, correlationId);

            // Step 4: Retrieve results
            log.debug("Retrieving results for document {} [correlationId={}]", docId, correlationId);
            String resultJson = raylightGet("/documents/" + docId + "/reports", token, correlationId);

            // Transform and cache
            ReportResults results = responseTransformer.transform(
                    resultJson,
                    job.getReportDefinition().getDisplayName(),
                    startTime
            );
            job.setCachedResults(results);
            job.setStatus(ReportStatus.COMPLETE);
            job.setCompletedAt(Instant.now());

            circuitBreaker.recordSuccess();
            log.info("Sync report {} completed in {}ms [correlationId={}]",
                    job.getReportDefinition().getReportCode(),
                    results.getMetadata().getExecutionTimeMs(),
                    correlationId);

            return job;

        } catch (WebClientResponseException e) {
            handleBoeError(job, e);
            return job;
        } catch (Exception e) {
            circuitBreaker.recordFailure();
            job.setStatus(ReportStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(Instant.now());
            log.error("Sync report {} failed [correlationId={}]",
                    job.getReportDefinition().getReportCode(), correlationId, e);
            return job;
        }
    }

    // ── Asynchronous flow ───────────────────────────────────────────────

    private ReportJob executeAsynchronous(ReportJob job) {
        String correlationId = job.getCorrelationId();
        String docId = job.getReportDefinition().getBoeDocumentId();

        try {
            String token = tokenManager.getToken(correlationId);

            // Submit report to BOE scheduler
            log.info("Submitting async report {} [correlationId={}]",
                    job.getReportDefinition().getReportCode(), correlationId);

            Map<String, Object> schedulePayload = new HashMap<>();
            schedulePayload.put("format", "webi");
            if (job.getParameters() != null && !job.getParameters().isEmpty()) {
                schedulePayload.put("parameters", buildParameterPayload(job.getParameters()));
            }

            String scheduleResponse = raylightPost(
                    "/documents/" + docId + "/schedules",
                    schedulePayload, token, correlationId
            );

            // Extract schedule ID from response
            // TODO: Parse actual Raylight schedule response structure
            String scheduleId = extractScheduleId(scheduleResponse);
            job.setBoeScheduleId(scheduleId);
            job.setStatus(ReportStatus.SUBMITTED);

            circuitBreaker.recordSuccess();
            log.info("Async report {} submitted, scheduleId={} [correlationId={}]",
                    job.getReportDefinition().getReportCode(), scheduleId, correlationId);

            return job;

        } catch (WebClientResponseException e) {
            handleBoeError(job, e);
            return job;
        } catch (Exception e) {
            circuitBreaker.recordFailure();
            job.setStatus(ReportStatus.FAILED);
            job.setErrorMessage("Failed to submit report: " + e.getMessage());
            job.setCompletedAt(Instant.now());
            log.error("Failed to submit async report {} [correlationId={}]",
                    job.getReportDefinition().getReportCode(), correlationId, e);
            return job;
        }
    }

    // ── HTTP helpers ────────────────────────────────────────────────────

    private String raylightGet(String path, String token, String correlationId) {
        return eagWebClient.get()
                .uri(boeConfig.getRaylightPath() + path)
                .header("X-SAP-LogonToken", token)
                .header("X-Correlation-Id", correlationId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private String raylightPut(String path, Object body, String token, String correlationId) {
        WebClient.RequestBodySpec spec = eagWebClient.put()
                .uri(boeConfig.getRaylightPath() + path)
                .header("X-SAP-LogonToken", token)
                .header("X-Correlation-Id", correlationId)
                .contentType(MediaType.APPLICATION_JSON);

        if (body != null) {
            return spec.bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        }
        return spec.retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private String raylightPost(String path, Object body, String token, String correlationId) {
        return eagWebClient.post()
                .uri(boeConfig.getRaylightPath() + path)
                .header("X-SAP-LogonToken", token)
                .header("X-Correlation-Id", correlationId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private Map<String, Object> buildParameterPayload(Map<String, String> parameters) {
        List<Map<String, Object>> paramList = new ArrayList<>();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            paramList.add(Map.of(
                    "name", entry.getKey(),
                    "answer", Map.of("values", Map.of("value", entry.getValue()))
            ));
        }
        return Map.of("parameters", Map.of("parameter", paramList));
    }

    private void handleBoeError(ReportJob job, WebClientResponseException e) {
        circuitBreaker.recordFailure();
        HttpStatusCode status = e.getStatusCode();

        if (status.value() == 401) {
            // Token may have expired mid-request — attempt re-auth on next call
            tokenManager.invalidate();
        }

        if (status.value() == 403) {
            throw new ReportAccessDeniedException(job.getCorrelationId());
        }

        job.setStatus(ReportStatus.FAILED);
        job.setErrorMessage("BOE returned HTTP " + status.value());
        job.setCompletedAt(Instant.now());
        log.error("BOE error HTTP {} for report {} [correlationId={}]",
                status.value(), job.getReportDefinition().getReportCode(), job.getCorrelationId());
    }

    private String extractScheduleId(String response) {
        // TODO: Parse the actual Raylight schedule response JSON
        //       to extract the schedule/instance ID.
        //       Placeholder — return a generated ID until the real response format is known.
        return "SCH-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String generateJobId() {
        return UUID.randomUUID().toString();
    }
}
