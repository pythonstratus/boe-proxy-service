package com.dstest.boe.proxy.service;

import com.dstest.boe.proxy.exception.ReportNotFoundException;
import com.dstest.boe.proxy.model.ReportDefinition;
import com.dstest.boe.proxy.model.RuntimeTier;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps Entity report codes to BOE Document IDs and metadata.
 *
 * <p>Currently backed by an in-memory map populated at startup. Can be
 * migrated to a database or config file as the report catalog grows.
 * Environment-specific document IDs are handled via Spring profiles.</p>
 *
 * <p>The frontend sends a reportCode like "MVIEW_SUMMARY". This service
 * resolves it to the BOE SI_ID, CUID, required parameters, and expected
 * runtime tier — the frontend never sees BOE-specific identifiers.</p>
 */
@Slf4j
@Service
public class ReportRegistry {

    private final Map<String, ReportDefinition> registry = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // TODO: Replace with database or config-file-based loading.
        //       These are placeholder definitions — update with actual
        //       BOE Document IDs per environment once known.

        register(ReportDefinition.builder()
                .reportCode("MVIEW_SUMMARY")
                .boeDocumentId("12345")
                .boeCuid("AX1bC2dE3fG4h")
                .displayName("Materialized View Summary Report")
                .expectedRuntimeTier(RuntimeTier.SLOW)
                .requiredParameters(List.of("TIN", "TAX_YEAR"))
                .build());

        register(ReportDefinition.builder()
                .reportCode("EVIEW_DETAIL")
                .boeDocumentId("67890")
                .boeCuid("BX5iJ6kL7mN8o")
                .displayName("Entity View Detail Report")
                .expectedRuntimeTier(RuntimeTier.SLOW)
                .requiredParameters(List.of("TIN"))
                .build());

        register(ReportDefinition.builder()
                .reportCode("QUICK_LOOKUP")
                .boeDocumentId("11111")
                .boeCuid("CQ9pR0sT1uV2w")
                .displayName("Quick Entity Lookup")
                .expectedRuntimeTier(RuntimeTier.FAST)
                .requiredParameters(List.of("TIN"))
                .build());

        log.info("Report registry initialized with {} report definitions", registry.size());
    }

    /**
     * Resolves a report code to its full definition.
     *
     * @param reportCode the Entity-facing report identifier
     * @param correlationId for tracing
     * @return the report definition
     * @throws ReportNotFoundException if the code is not in the registry
     */
    public ReportDefinition lookup(String reportCode, String correlationId) {
        ReportDefinition definition = registry.get(reportCode.toUpperCase());
        if (definition == null) {
            throw new ReportNotFoundException(reportCode, correlationId);
        }
        return definition;
    }

    /**
     * Validates that all required parameters for a report are present.
     *
     * @param definition the report definition
     * @param parameters the user-provided parameters
     * @param correlationId for tracing
     * @throws IllegalArgumentException if required parameters are missing
     */
    public void validateParameters(ReportDefinition definition,
                                   Map<String, String> parameters,
                                   String correlationId) {
        if (definition.getRequiredParameters() == null || definition.getRequiredParameters().isEmpty()) {
            return;
        }

        List<String> missing = definition.getRequiredParameters().stream()
                .filter(param -> parameters == null || !parameters.containsKey(param)
                        || parameters.get(param) == null || parameters.get(param).isBlank())
                .toList();

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    "Missing required parameters for report " + definition.getReportCode()
                            + ": " + String.join(", ", missing)
            );
        }
    }

    /**
     * Returns all registered report definitions. Useful for admin/debug endpoints.
     */
    public Map<String, ReportDefinition> getAllDefinitions() {
        return Map.copyOf(registry);
    }

    private void register(ReportDefinition definition) {
        registry.put(definition.getReportCode().toUpperCase(), definition);
    }
}
