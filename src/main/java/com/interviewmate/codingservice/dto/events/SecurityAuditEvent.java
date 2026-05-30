package com.interviewmate.codingservice.dto.events;

import java.time.Instant;
import java.util.UUID;

import com.interviewmate.codingservice.constants.AuditEventType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SecurityAuditEvent {

    private final String         eventId;
    private final AuditEventType eventType;
    private final String         userId;
    private final String         role;
    private final String         httpMethod;
    private final String         path;
    private final String         controllerMethod;
    private final int            statusCode;
    private final long           durationMs;
    private final String         serviceName;
    private final String         message;

    @Builder.Default
    private final String timestamp = Instant.now().toString();

    public static SecurityAuditEvent build(AuditEventType type,
                                           String userId,
                                           String role,
                                           String httpMethod,
                                           String path,
                                           String controllerMethod,
                                           int statusCode,
                                           long durationMs,
                                           String serviceName,
                                           String message) {
        return SecurityAuditEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(type)
            .userId(userId)
            .role(role)
            .httpMethod(httpMethod)
            .path(path)
            .controllerMethod(controllerMethod)
            .statusCode(statusCode)
            .durationMs(durationMs)
            .serviceName(serviceName)
            .message(message)
            .build();
    }
}