package com.dstest.boe.proxy.dto.response;

import com.dstest.boe.proxy.model.ColumnDefinition;
import com.dstest.boe.proxy.model.ReportResults;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReportResultsResponse {

    private String jobId;
    private List<ColumnDefinition> columns;
    private List<List<Object>> rows;
    private ReportResults.ResultMetadata metadata;
}
