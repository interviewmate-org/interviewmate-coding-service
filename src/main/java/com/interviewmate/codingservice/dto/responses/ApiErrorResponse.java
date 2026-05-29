package com.interviewmate.codingservice.dto.responses;

import java.time.Instant;

import org.springframework.http.HttpStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiErrorResponse {

    private final int    status;
    private final String error;
    private final String message;
    private final String path;

    @Builder.Default
    private final String timestamp = Instant.now().toString();

    // Factory methods so callers don't build manually
    public static ApiErrorResponse of(HttpStatus status, String message, String path) {
        return ApiErrorResponse.builder()
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(message)
            .path(path)
            .build();
    }

    public static ApiErrorResponse of(HttpStatus status, String message) {
        return of(status, message, null);
    }
}