package com.dstest.boe.proxy.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReportCancelResponse {

    private String jobId;
    private boolean cancelled;
    private String message;
}
