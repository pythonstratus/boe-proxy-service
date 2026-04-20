package com.dstest.boe.proxy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ReportRunRequest {

    @NotBlank(message = "reportCode is required")
    private String reportCode;

    /**
     * Key-value pairs of report parameters (e.g., {"TIN": "123456789", "TAX_YEAR": "2025"}).
     */
    private Map<String, String> parameters;
}
