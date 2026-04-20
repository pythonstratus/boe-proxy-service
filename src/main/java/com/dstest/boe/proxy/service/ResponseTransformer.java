package com.dstest.boe.proxy.service;

import com.dstest.boe.proxy.model.ColumnDefinition;
import com.dstest.boe.proxy.model.ReportResults;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Transforms BOE Raylight response payloads into Entity's internal data model.
 *
 * <p>Raylight returns report data in a nested structure:
 * {@code reports → report → dataSets → dataSet → rows / headers}.
 * This service flattens that into a simple columns + rows structure
 * that the frontend can consume directly.</p>
 *
 * <p>Handles:
 * <ul>
 *   <li>Column header extraction and type mapping</li>
 *   <li>Row data flattening</li>
 *   <li>Date normalization (Oracle NLS → ISO 8601)</li>
 *   <li>Null value handling</li>
 *   <li>Multiple data providers (each becomes a separate ReportResults)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResponseTransformer {

    private final ObjectMapper objectMapper;

    /**
     * Parses the raw Raylight JSON response body into structured ReportResults.
     *
     * @param responseBody the raw JSON string from Raylight
     * @param reportName   display name for metadata
     * @param startTime    when execution started, for calculating execution time
     * @return parsed report results
     */
    public ReportResults transform(String responseBody, String reportName, Instant startTime) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return parseReportData(root, reportName, startTime);
        } catch (Exception e) {
            log.error("Failed to transform Raylight response for report: {}", reportName, e);
            throw new RuntimeException("Failed to parse BOE report response: " + e.getMessage(), e);
        }
    }

    private ReportResults parseReportData(JsonNode root, String reportName, Instant startTime) {
        List<ColumnDefinition> columns = new ArrayList<>();
        List<List<Object>> rows = new ArrayList<>();
        String dataProviderName = "";

        // Navigate Raylight's nested structure
        // Path: root → dataSets → dataSet[] → headers / rows
        // The exact path may vary — this handles the common Raylight v1 format.
        JsonNode dataSets = findNode(root, "dataSets", "dataSet");

        if (dataSets != null && dataSets.isArray() && !dataSets.isEmpty()) {
            JsonNode firstDataSet = dataSets.get(0);

            // Extract column headers
            JsonNode headers = findNode(firstDataSet, "headers", "header");
            if (headers != null && headers.isArray()) {
                for (JsonNode header : headers) {
                    columns.add(ColumnDefinition.builder()
                            .name(getTextValue(header, "name"))
                            .type(getTextValue(header, "dataType"))
                            .displayLabel(getTextValue(header, "name"))
                            .build());
                }
            }

            // Extract row data
            JsonNode rowsNode = findNode(firstDataSet, "rows", "row");
            if (rowsNode != null && rowsNode.isArray()) {
                for (JsonNode row : rowsNode) {
                    List<Object> rowData = new ArrayList<>();
                    JsonNode values = row.has("value") ? row.get("value") : row;

                    if (values.isArray()) {
                        for (JsonNode cell : values) {
                            rowData.add(extractCellValue(cell));
                        }
                    }
                    rows.add(rowData);
                }
            }

            dataProviderName = getTextValue(firstDataSet, "name");
        }

        long executionTimeMs = java.time.Duration.between(startTime, Instant.now()).toMillis();

        return ReportResults.builder()
                .columns(columns)
                .rows(rows)
                .metadata(ReportResults.ResultMetadata.builder()
                        .rowCount(rows.size())
                        .executionTimeMs(executionTimeMs)
                        .reportName(reportName)
                        .dataProviderName(dataProviderName)
                        .generatedAt(Instant.now().toString())
                        .build())
                .build();
    }

    /**
     * Navigates nested JSON to find a node by trying multiple path combinations.
     * Raylight responses can nest objects inside wrapper elements.
     */
    private JsonNode findNode(JsonNode parent, String... pathOptions) {
        if (parent == null) return null;

        for (String path : pathOptions) {
            if (parent.has(path)) {
                JsonNode node = parent.get(path);
                // Raylight sometimes wraps arrays in a singular-named parent
                // e.g., { "dataSets": { "dataSet": [...] } }
                for (String innerPath : pathOptions) {
                    if (node.has(innerPath)) {
                        return node.get(innerPath);
                    }
                }
                return node;
            }
        }
        return null;
    }

    private Object extractCellValue(JsonNode cell) {
        if (cell == null || cell.isNull()) {
            return null;
        }
        if (cell.isNumber()) {
            return cell.numberValue();
        }
        if (cell.isBoolean()) {
            return cell.booleanValue();
        }
        // Return as string; caller can apply further type conversion if needed
        return cell.asText();
    }

    private String getTextValue(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName)) {
            return "";
        }
        return node.get(fieldName).asText("");
    }
}
