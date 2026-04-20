package com.dstest.boe.proxy.service;

import com.dstest.boe.proxy.config.BoeConfig;
import com.dstest.boe.proxy.config.ProxyConfig;
import com.dstest.boe.proxy.model.ReportJob;
import com.dstest.boe.proxy.model.ReportResults;
import com.dstest.boe.proxy.model.ReportStatus;
import com.dstest.boe.proxy.model.RuntimeTier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Background polling engine that checks BOE for the status of async report jobs.
 *
 * <p>Runs on a fixed schedule (every 3 seconds by default). Iterates over all
 * jobs in the tracker with status SUBMITTED or RUNNING, checks their status
 * against BOE via Raylight, and fetches results when complete.</p>
 *
 * <p>The frontend polls the proxy's own status endpoint — this engine is the
 * only component that talks to BOE during async execution. Polling intervals
 * are tiered: FAST reports are polled more aggressively than SLOW reports.</p>
 *
 * <p>Also handles job cleanup — removing stale jobs from the tracker after
 * the configured cleanup threshold to prevent memory leaks.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PollingEngine {

    private final ReportExecutor reportExecutor;
    private final BoeTokenManager tokenManager;
    private final CircuitBreakerService circuitBreaker;
    private final ResponseTransformer responseTransformer;
    private final WebClient eagWebClient;
    private final BoeConfig boeConfig;
    private final ProxyConfig proxyConfig;

    /**
     * Main polling loop. Runs every 3 seconds.
     * Checks all pending async jobs and cleans up stale entries.
     */
    @Scheduled(fixedDelayString = "${proxy.polling.fast-interval:3000}")
    public void pollPendingJobs() {
        Map<String, ReportJob> jobTracker = reportExecutor.getJobTracker();

        for (Map.Entry<String, ReportJob> entry : jobTracker.entrySet()) {
            ReportJob job = entry.getValue();

            if (isPending(job)) {
                if (shouldPollNow(job)) {
                    pollJobStatus(job);
                }
            }
        }

        // Cleanup stale jobs
        cleanupStaleJobs(jobTracker);
    }

    private boolean isPending(ReportJob job) {
        return job.getStatus() == ReportStatus.SUBMITTED
                || job.getStatus() == ReportStatus.RUNNING;
    }

    /**
     * Determines whether enough time has elapsed since the last poll
     * based on the report's runtime tier.
     */
    private boolean shouldPollNow(ReportJob job) {
        Instant lastPoll = job.getLastPolledAt();
        if (lastPoll == null) {
            // First poll — respect initial delay
            Duration elapsed = Duration.between(job.getSubmittedAt(), Instant.now());
            return elapsed.compareTo(proxyConfig.getPolling().getInitialDelay()) >= 0;
        }

        Duration interval = getPollingInterval(job);
        Duration sinceLast = Duration.between(lastPoll, Instant.now());
        return sinceLast.compareTo(interval) >= 0;
    }

    private Duration getPollingInterval(ReportJob job) {
        if (job.getReportDefinition().getExpectedRuntimeTier() == RuntimeTier.FAST) {
            return proxyConfig.getPolling().getFastInterval();
        }
        return proxyConfig.getPolling().getSlowInterval();
    }

    /**
     * Polls BOE for a single job's status. If complete, fetches and caches results.
     */
    private void pollJobStatus(ReportJob job) {
        String correlationId = job.getCorrelationId();
        String docId = job.getReportDefinition().getBoeDocumentId();
        String scheduleId = job.getBoeScheduleId();

        // Check max poll limits
        if (isTimedOut(job)) {
            job.setStatus(ReportStatus.TIMED_OUT);
            job.setCompletedAt(Instant.now());
            job.setErrorMessage("Report execution exceeded maximum poll duration");
            log.warn("Job {} timed out after {} polls, {}s elapsed [correlationId={}]",
                    job.getJobId(), job.getPollCount(), job.getElapsedSeconds(), correlationId);
            return;
        }

        try {
            circuitBreaker.checkState(correlationId);
            String token = tokenManager.getToken(correlationId);

            // Check schedule status
            String statusResponse = eagWebClient.get()
                    .uri(boeConfig.getRaylightPath() + "/documents/" + docId
                            + "/schedules/" + scheduleId)
                    .header("X-SAP-LogonToken", token)
                    .header("X-Correlation-Id", correlationId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            job.setLastPolledAt(Instant.now());
            job.setPollCount(job.getPollCount() + 1);

            String boeStatus = parseJobStatus(statusResponse);

            switch (boeStatus.toLowerCase()) {
                case "running", "pending" -> {
                    job.setStatus(ReportStatus.RUNNING);
                    log.debug("Job {} still running, poll #{} [correlationId={}]",
                            job.getJobId(), job.getPollCount(), correlationId);
                }
                case "complete", "success" -> {
                    fetchAndCacheResults(job, token);
                }
                case "failed", "error" -> {
                    job.setStatus(ReportStatus.FAILED);
                    job.setCompletedAt(Instant.now());
                    job.setErrorMessage("BOE report execution failed");
                    log.error("Job {} failed on BOE side [correlationId={}]",
                            job.getJobId(), correlationId);
                }
                default -> {
                    job.setStatus(ReportStatus.RUNNING);
                    log.debug("Job {} returned unknown status: {} [correlationId={}]",
                            job.getJobId(), boeStatus, correlationId);
                }
            }

            circuitBreaker.recordSuccess();

        } catch (Exception e) {
            circuitBreaker.recordFailure();
            job.setLastPolledAt(Instant.now());
            job.setPollCount(job.getPollCount() + 1);
            log.error("Error polling job {} [correlationId={}]", job.getJobId(), correlationId, e);
            // Don't fail the job on a single poll error — it may be transient.
            // The timeout mechanism will catch persistent failures.
        }
    }

    /**
     * Fetches completed report results from BOE and caches them in the job.
     */
    private void fetchAndCacheResults(ReportJob job, String token) {
        String correlationId = job.getCorrelationId();
        String docId = job.getReportDefinition().getBoeDocumentId();
        String scheduleId = job.getBoeScheduleId();

        try {
            String resultJson = eagWebClient.get()
                    .uri(boeConfig.getRaylightPath() + "/documents/" + docId
                            + "/schedules/" + scheduleId + "/reports")
                    .header("X-SAP-LogonToken", token)
                    .header("X-Correlation-Id", correlationId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            ReportResults results = responseTransformer.transform(
                    resultJson,
                    job.getReportDefinition().getDisplayName(),
                    job.getSubmittedAt()
            );

            job.setCachedResults(results);
            job.setStatus(ReportStatus.COMPLETE);
            job.setCompletedAt(Instant.now());
            log.info("Job {} completed, {} rows in {}s [correlationId={}]",
                    job.getJobId(),
                    results.getMetadata().getRowCount(),
                    job.getElapsedSeconds(),
                    correlationId);

        } catch (Exception e) {
            job.setStatus(ReportStatus.FAILED);
            job.setCompletedAt(Instant.now());
            job.setErrorMessage("Failed to retrieve results: " + e.getMessage());
            log.error("Failed to fetch results for job {} [correlationId={}]",
                    job.getJobId(), correlationId, e);
        }
    }

    private boolean isTimedOut(ReportJob job) {
        Duration elapsed = Duration.between(job.getSubmittedAt(), Instant.now());
        boolean durationExceeded = elapsed.compareTo(proxyConfig.getPolling().getMaxDuration()) >= 0;
        boolean attemptsExceeded = job.getPollCount() >= proxyConfig.getPolling().getMaxAttempts();
        return durationExceeded || attemptsExceeded;
    }

    /**
     * Removes completed, failed, cancelled, or timed-out jobs that have been in the
     * tracker longer than the configured cleanup threshold.
     */
    private void cleanupStaleJobs(Map<String, ReportJob> jobTracker) {
        Instant cutoff = Instant.now().minus(proxyConfig.getJobTracker().getCleanupAfter());

        jobTracker.entrySet().removeIf(entry -> {
            ReportJob job = entry.getValue();
            boolean isTerminal = job.getStatus() == ReportStatus.COMPLETE
                    || job.getStatus() == ReportStatus.FAILED
                    || job.getStatus() == ReportStatus.CANCELLED
                    || job.getStatus() == ReportStatus.TIMED_OUT;
            boolean isStale = job.getSubmittedAt().isBefore(cutoff);

            if (isTerminal && isStale) {
                log.debug("Cleaning up stale job {}", job.getJobId());
                return true;
            }
            return false;
        });
    }

    /**
     * Parses the BOE job status from the Raylight schedule response.
     */
    private String parseJobStatus(String response) {
        // TODO: Parse the actual Raylight schedule status response JSON.
        //       Expected structure: { "status": "running" | "complete" | "failed" }
        //       Placeholder — extract from raw JSON until real format is confirmed.
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(response);
            if (root.has("status")) {
                return root.get("status").asText("running");
            }
        } catch (Exception e) {
            log.warn("Failed to parse BOE status response, defaulting to 'running'", e);
        }
        return "running";
    }
}
