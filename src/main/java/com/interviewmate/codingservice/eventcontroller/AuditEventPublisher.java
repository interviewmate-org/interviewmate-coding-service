package com.interviewmate.codingservice.eventcontroller;

import com.interviewmate.codingservice.dto.events.SecurityAuditEvent;

public interface AuditEventPublisher {
    void publish(SecurityAuditEvent event);
}