package com.dstest.boe.proxy.controller;

import com.dstest.boe.proxy.exception.ReportNotFoundException;
import com.dstest.boe.proxy.model.*;
import com.dstest.boe.proxy.service.ReportExecutor;
import com.dstest.boe.proxy.service.ReportRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReportRegistry reportRegistry;

    @MockBean
    private ReportExecutor reportExecutor;

    @Test
    @DisplayName("POST /api/reports/run returns 202 for async submission")
    void testRunReportAsync() throws Exception {
        ReportDefinition definition = ReportDefinition.builder()
                .reportCode("MVIEW_SUMMARY")
                .boeDocumentId("12345")
                .expectedRuntimeTier(RuntimeTier.SLOW)
                .requiredParameters(List.of("TIN"))
                .build();

        ReportJob job = ReportJob.builder()
                .jobId("test-job-1")
                .reportDefinition(definition)
                .status(ReportStatus.SUBMITTED)
                .submittedAt(Instant.now())
                .build();

        when(reportRegistry.lookup(eq("MVIEW_SUMMARY"), anyString())).thenReturn(definition);
        when(reportExecutor.executeReport(eq(definition), anyMap(), anyString(), any()))
                .thenReturn(job);

        String requestJson = """
            {
              "reportCode": "MVIEW_SUMMARY",
              "parameters": { "TIN": "123456789" }
            }
            """;

        mockMvc.perform(post("/api/reports/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value("test-job-1"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.reportCode").value("MVIEW_SUMMARY"));
    }

    @Test
    @DisplayName("POST /api/reports/run returns 200 for sync completion")
    void testRunReportSync() throws Exception {
        ReportDefinition definition = ReportDefinition.builder()
                .reportCode("QUICK_LOOKUP")
                .boeDocumentId("11111")
                .expectedRuntimeTier(RuntimeTier.FAST)
                .requiredParameters(List.of("TIN"))
                .build();

        ReportJob job = ReportJob.builder()
                .jobId("test-job-2")
                .reportDefinition(definition)
                .status(ReportStatus.COMPLETE)
                .submittedAt(Instant.now())
                .completedAt(Instant.now())
                .build();

        when(reportRegistry.lookup(eq("QUICK_LOOKUP"), anyString())).thenReturn(definition);
        when(reportExecutor.executeReport(eq(definition), anyMap(), anyString(), any()))
                .thenReturn(job);

        String requestJson = """
            {
              "reportCode": "QUICK_LOOKUP",
              "parameters": { "TIN": "123456789" }
            }
            """;

        mockMvc.perform(post("/api/reports/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("test-job-2"))
                .andExpect(jsonPath("$.status").value("COMPLETE"));
    }

    @Test
    @DisplayName("POST /api/reports/run returns 404 for unknown report code")
    void testRunReportUnknownCode() throws Exception {
        when(reportRegistry.lookup(eq("FAKE_REPORT"), anyString()))
                .thenThrow(new ReportNotFoundException("FAKE_REPORT", "test-corr"));

        String requestJson = """
            {
              "reportCode": "FAKE_REPORT",
              "parameters": {}
            }
            """;

        mockMvc.perform(post("/api/reports/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("REPORT_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/reports/{jobId}/status returns job status")
    void testGetStatus() throws Exception {
        ReportJob job = ReportJob.builder()
                .jobId("test-job-3")
                .status(ReportStatus.RUNNING)
                .submittedAt(Instant.now().minusSeconds(10))
                .pollCount(3)
                .reportDefinition(ReportDefinition.builder()
                        .reportCode("MVIEW_SUMMARY")
                        .build())
                .build();

        when(reportExecutor.getJob("test-job-3")).thenReturn(job);

        mockMvc.perform(get("/api/reports/test-job-3/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("test-job-3"))
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.pollCount").value(3));
    }

    @Test
    @DisplayName("GET /api/reports/{jobId}/status returns 404 for unknown job")
    void testGetStatusUnknownJob() throws Exception {
        when(reportExecutor.getJob("nonexistent")).thenReturn(null);

        mockMvc.perform(get("/api/reports/nonexistent/status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("JOB_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/reports/{jobId}/results returns 409 if not complete")
    void testGetResultsNotComplete() throws Exception {
        ReportJob job = ReportJob.builder()
                .jobId("test-job-4")
                .status(ReportStatus.RUNNING)
                .submittedAt(Instant.now())
                .build();

        when(reportExecutor.getJob("test-job-4")).thenReturn(job);

        mockMvc.perform(get("/api/reports/test-job-4/results"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /api/reports/{jobId}/results returns data when complete")
    void testGetResultsComplete() throws Exception {
        ReportResults results = ReportResults.builder()
                .columns(List.of(
                        ColumnDefinition.builder().name("TIN").type("String").build()
                ))
                .rows(List.of(
                        List.of((Object) "123456789")
                ))
                .metadata(ReportResults.ResultMetadata.builder()
                        .rowCount(1)
                        .executionTimeMs(500)
                        .reportName("Test")
                        .generatedAt(Instant.now().toString())
                        .build())
                .build();

        ReportJob job = ReportJob.builder()
                .jobId("test-job-5")
                .status(ReportStatus.COMPLETE)
                .submittedAt(Instant.now())
                .cachedResults(results)
                .build();

        when(reportExecutor.getJob("test-job-5")).thenReturn(job);

        mockMvc.perform(get("/api/reports/test-job-5/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("test-job-5"))
                .andExpect(jsonPath("$.columns[0].name").value("TIN"))
                .andExpect(jsonPath("$.rows[0][0]").value("123456789"))
                .andExpect(jsonPath("$.metadata.rowCount").value(1));
    }

    @Test
    @DisplayName("DELETE /api/reports/{jobId} cancels a running job")
    void testCancelJob() throws Exception {
        ReportJob job = ReportJob.builder()
                .jobId("test-job-6")
                .status(ReportStatus.RUNNING)
                .submittedAt(Instant.now())
                .build();

        when(reportExecutor.getJob("test-job-6")).thenReturn(job);
        when(reportExecutor.cancelJob("test-job-6")).thenReturn(true);

        mockMvc.perform(delete("/api/reports/test-job-6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("test-job-6"))
                .andExpect(jsonPath("$.cancelled").value(true));
    }
}
