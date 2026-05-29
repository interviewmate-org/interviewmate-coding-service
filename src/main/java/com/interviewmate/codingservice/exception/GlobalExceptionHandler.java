package com.interviewmate.codingservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.interviewmate.codingservice.dto.responses.ApiErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Access denied (method-level @PreAuthorize failures)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, ServerHttpRequest request) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiErrorResponse.of(
                HttpStatus.FORBIDDEN,
                "Access denied",
                request.getURI().getPath()
            ));
    }

    // Catch-all
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex, ServerHttpRequest request) {

        log.error("Unhandled exception at [{}]", request.getURI().getPath(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                request.getURI().getPath()
            ));
    }
}