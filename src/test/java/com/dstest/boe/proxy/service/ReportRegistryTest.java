package com.dstest.boe.proxy.service;

import com.dstest.boe.proxy.exception.ReportNotFoundException;
import com.dstest.boe.proxy.model.ReportDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ReportRegistryTest {

    private ReportRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ReportRegistry();
        registry.init();
    }

    @Test
    @DisplayName("lookup resolves a valid report code")
    void testLookupValid() {
        ReportDefinition definition = registry.lookup("MVIEW_SUMMARY", "test-corr-1");

        assertThat(definition).isNotNull();
        assertThat(definition.getReportCode()).isEqualTo("MVIEW_SUMMARY");
        assertThat(definition.getBoeDocumentId()).isEqualTo("12345");
        assertThat(definition.getRequiredParameters()).contains("TIN", "TAX_YEAR");
    }

    @Test
    @DisplayName("lookup is case-insensitive")
    void testLookupCaseInsensitive() {
        ReportDefinition definition = registry.lookup("mview_summary", "test-corr-2");
        assertThat(definition).isNotNull();
        assertThat(definition.getReportCode()).isEqualTo("MVIEW_SUMMARY");
    }

    @Test
    @DisplayName("lookup throws ReportNotFoundException for unknown code")
    void testLookupUnknown() {
        assertThatThrownBy(() -> registry.lookup("NONEXISTENT", "test-corr-3"))
                .isInstanceOf(ReportNotFoundException.class)
                .hasMessageContaining("NONEXISTENT");
    }

    @Test
    @DisplayName("validateParameters passes when all required params present")
    void testValidateParametersValid() {
        ReportDefinition definition = registry.lookup("MVIEW_SUMMARY", "test-corr-4");
        Map<String, String> params = Map.of("TIN", "123456789", "TAX_YEAR", "2025");

        assertThatCode(() -> registry.validateParameters(definition, params, "test-corr-4"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateParameters throws when required params missing")
    void testValidateParametersMissing() {
        ReportDefinition definition = registry.lookup("MVIEW_SUMMARY", "test-corr-5");
        Map<String, String> params = Map.of("TIN", "123456789");

        assertThatThrownBy(() -> registry.validateParameters(definition, params, "test-corr-5"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TAX_YEAR");
    }

    @Test
    @DisplayName("validateParameters throws when params map is null")
    void testValidateParametersNull() {
        ReportDefinition definition = registry.lookup("MVIEW_SUMMARY", "test-corr-6");

        assertThatThrownBy(() -> registry.validateParameters(definition, null, "test-corr-6"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("validateParameters throws when param value is blank")
    void testValidateParametersBlank() {
        ReportDefinition definition = registry.lookup("MVIEW_SUMMARY", "test-corr-7");
        Map<String, String> params = Map.of("TIN", "123456789", "TAX_YEAR", "");

        assertThatThrownBy(() -> registry.validateParameters(definition, params, "test-corr-7"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TAX_YEAR");
    }

    @Test
    @DisplayName("getAllDefinitions returns all registered reports")
    void testGetAllDefinitions() {
        Map<String, ReportDefinition> all = registry.getAllDefinitions();
        assertThat(all).hasSize(3);
        assertThat(all).containsKeys("MVIEW_SUMMARY", "EVIEW_DETAIL", "QUICK_LOOKUP");
    }
}
