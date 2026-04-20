package com.dstest.boe.proxy.service;

import com.dstest.boe.proxy.model.ReportResults;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class ResponseTransformerTest {

    private ResponseTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new ResponseTransformer(new ObjectMapper());
    }

    @Test
    @DisplayName("transforms Raylight response with headers and rows")
    void testTransformBasicResponse() {
        String raylightJson = """
            {
              "dataSets": {
                "dataSet": [
                  {
                    "name": "TestProvider",
                    "headers": {
                      "header": [
                        { "name": "TIN", "dataType": "String" },
                        { "name": "AMOUNT", "dataType": "Number" }
                      ]
                    },
                    "rows": {
                      "row": [
                        { "value": ["123456789", 1500.50] },
                        { "value": ["987654321", 2300.00] }
                      ]
                    }
                  }
                ]
              }
            }
            """;

        Instant startTime = Instant.now().minusSeconds(5);
        ReportResults results = transformer.transform(raylightJson, "Test Report", startTime);

        assertThat(results).isNotNull();
        assertThat(results.getColumns()).hasSize(2);
        assertThat(results.getColumns().get(0).getName()).isEqualTo("TIN");
        assertThat(results.getColumns().get(1).getName()).isEqualTo("AMOUNT");
        assertThat(results.getRows()).hasSize(2);
        assertThat(results.getMetadata().getRowCount()).isEqualTo(2);
        assertThat(results.getMetadata().getReportName()).isEqualTo("Test Report");
        assertThat(results.getMetadata().getDataProviderName()).isEqualTo("TestProvider");
        assertThat(results.getMetadata().getExecutionTimeMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("handles empty data set gracefully")
    void testTransformEmptyDataSet() {
        String raylightJson = """
            {
              "dataSets": {
                "dataSet": [
                  {
                    "name": "EmptyProvider",
                    "headers": {
                      "header": [
                        { "name": "COL1", "dataType": "String" }
                      ]
                    },
                    "rows": {
                      "row": []
                    }
                  }
                ]
              }
            }
            """;

        ReportResults results = transformer.transform(raylightJson, "Empty Report", Instant.now());

        assertThat(results.getColumns()).hasSize(1);
        assertThat(results.getRows()).isEmpty();
        assertThat(results.getMetadata().getRowCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("handles missing dataSets node")
    void testTransformMissingDataSets() {
        String raylightJson = """
            {
              "status": "success"
            }
            """;

        ReportResults results = transformer.transform(raylightJson, "No Data", Instant.now());

        assertThat(results.getColumns()).isEmpty();
        assertThat(results.getRows()).isEmpty();
    }

    @Test
    @DisplayName("handles null cell values")
    void testTransformNullValues() {
        String raylightJson = """
            {
              "dataSets": {
                "dataSet": [
                  {
                    "name": "NullProvider",
                    "headers": {
                      "header": [
                        { "name": "FIELD1", "dataType": "String" }
                      ]
                    },
                    "rows": {
                      "row": [
                        { "value": [null] }
                      ]
                    }
                  }
                ]
              }
            }
            """;

        ReportResults results = transformer.transform(raylightJson, "Null Test", Instant.now());

        assertThat(results.getRows()).hasSize(1);
        assertThat(results.getRows().get(0).get(0)).isNull();
    }

    @Test
    @DisplayName("throws on malformed JSON")
    void testTransformMalformedJson() {
        assertThatThrownBy(() ->
                transformer.transform("not json", "Bad Report", Instant.now()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to parse");
    }
}
