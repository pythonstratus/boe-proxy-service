package com.dstest.boe.proxy.exception;

import com.dstest.boe.proxy.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReportNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReportNotFound(ReportNotFoundException ex) {
        log.warn("Report not found: {} [correlationId={}]", ex.getMessage(), ex.getCorrelationId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse(ex));
    }

    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleJobNotFound(JobNotFoundException ex) {
        log.warn("Job not found: {} [correlationId={}]", ex.getMessage(), ex.getCorrelationId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse(ex));
    }

    @ExceptionHandler(ReportAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(ReportAccessDeniedException ex) {
        log.warn("Access denied: {} [correlationId={}]", ex.getMessage(), ex.getCorrelationId());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(buildErrorResponse(ex));
    }

    @ExceptionHandler(BoeAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthFailure(BoeAuthenticationException ex) {
        log.error("BOE authentication failed: {} [correlationId={}]", ex.getMessage(), ex.getCorrelationId());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(buildErrorResponse(ex));
    }

    @ExceptionHandler(BoeServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(BoeServiceUnavailableException ex) {
        log.error("BOE service unavailable: {} [correlationId={}]", ex.getMessage(), ex.getCorrelationId());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildErrorResponse(ex));
    }

    @ExceptionHandler(BoeProxyException.class)
    public ResponseEntity<ErrorResponse> handleProxyException(BoeProxyException ex) {
        log.error("Proxy error: {} [correlationId={}]", ex.getMessage(), ex.getCorrelationId(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse(ex));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");

        log.warn("Validation error: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .error("VALIDATION_ERROR")
                        .message(message)
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .error("INTERNAL_ERROR")
                        .message("An unexpected error occurred")
                        .build());
    }

    private ErrorResponse buildErrorResponse(BoeProxyException ex) {
        return ErrorResponse.builder()
                .error(ex.getErrorCode())
                .message(ex.getMessage())
                .correlationId(ex.getCorrelationId())
                .build();
    }
}
