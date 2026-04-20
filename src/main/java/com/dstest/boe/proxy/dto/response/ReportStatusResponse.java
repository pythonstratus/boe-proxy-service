package com.dstest.boe.proxy.dto.response;

import com.dstest.boe.proxy.model.ReportStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReportStatusResponse {

    private String jobId;
    private ReportStatus status;
    private long elapsedSeconds;
    private int pollCount;
    private String message;
}
