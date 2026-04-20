package com.dstest.boe.proxy.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ReportResults {

    /**
     * Column definitions describing the result set structure.
     */
    private List<ColumnDefinition> columns;

    /**
     * Row data — each row is a list of values aligned with columns.
     */
    private List<List<Object>> rows;

    /**
     * Metadata about the report execution.
     */
    private ResultMetadata metadata;

    @Data
    @Builder
    public static class ResultMetadata {
        private int rowCount;
        private long executionTimeMs;
        private String reportName;
        private String dataProviderName;
        private String generatedAt;
    }
}
