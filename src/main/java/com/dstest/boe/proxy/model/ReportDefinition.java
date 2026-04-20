package com.dstest.boe.proxy.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReportDefinition {

    /**
     * Entity's internal report identifier (e.g., "MVIEW_SUMMARY", "EVIEW_DETAIL").
     * This is what the frontend sends.
     */
    private String reportCode;

    /**
     * BOE Document SI_ID (numeric). Used by most Raylight endpoints.
     */
    private String boeDocumentId;

    /**
     * BOE Document CUID (alphanumeric). Some Raylight endpoints accept this instead.
     */
    private String boeCuid;

    /**
     * Human-readable display name for the report.
     */
    private String displayName;

    /**
     * Expected runtime classification. Determines whether the proxy
     * uses synchronous or asynchronous execution.
     */
    private RuntimeTier expectedRuntimeTier;

    /**
     * List of parameter names required for this report (e.g., ["TIN", "TAX_YEAR"]).
     * Used for input validation before calling BOE.
     */
    private List<String> requiredParameters;

    /**
     * Optional timeout override in seconds. If null, uses the global proxy timeout.
     */
    private Integer timeoutOverrideSeconds;
}
