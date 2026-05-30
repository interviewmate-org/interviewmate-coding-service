package com.interviewmate.codingservice.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.interviewmate.codingservice.constants.AuditEventType;
import com.interviewmate.codingservice.dto.events.SecurityAuditEvent;
import com.interviewmate.codingservice.eventcontroller.AuditEventPublisher;
import com.interviewmate.codingservice.securityconfig.filter.GatewayAuthFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SecurityLoggingAspect {

    private final AuditEventPublisher auditPublisher;

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object logControllerAccess(ProceedingJoinPoint pjp) throws Throwable {

        String controllerMethod = pjp.getSignature().toShortString();

        //Case 1: @PreAuthorize denied BEFORE method returns Mono
        Object result;
        try {
            result = pjp.proceed();
        } catch (AccessDeniedException ex) {
            return Mono.deferContextual(ctx -> {
                String userId = ctx.getOrDefault(GatewayAuthFilter.CTX_USER_ID, "unknown");
                String role   = ctx.getOrDefault(GatewayAuthFilter.CTX_ROLE,    "unknown");
                String httpMethod = ctx.getOrDefault(GatewayAuthFilter.CTX_HTTP_METHOD, "unknown");
                String path       = ctx.getOrDefault(GatewayAuthFilter.CTX_PATH,        "unknown");

                log.warn("DENIED → {} | userId={} role={}", controllerMethod, userId, role);

                auditPublisher.publish(SecurityAuditEvent.build(
                    AuditEventType.ACCESS_DENIED,
                    userId, role, httpMethod, path,
                    controllerMethod, 403, 0,
                    serviceName, "PreAuthorize denied: " + ex.getMessage()
                ));

                return Mono.<Object>error(ex);
            });
        }

        //  Case 2: Mono returned — wrap with context-aware operators 
        if (result instanceof Mono<?> mono) {
            return Mono.deferContextual(ctx -> {
                String userId = ctx.getOrDefault(GatewayAuthFilter.CTX_USER_ID, "unknown");
                String role   = ctx.getOrDefault(GatewayAuthFilter.CTX_ROLE,    "unknown");
                String httpMethod = ctx.getOrDefault(GatewayAuthFilter.CTX_HTTP_METHOD, "unknown");
                String path       = ctx.getOrDefault(GatewayAuthFilter.CTX_PATH,        "unknown");

                MDC.put("userId", userId);
                MDC.put("role",   role);

                log.info("ACCESS → {} | userId={} role={}", controllerMethod, userId, role);

                auditPublisher.publish(SecurityAuditEvent.build(
                    AuditEventType.ACCESS_GRANTED,
                    userId, role, httpMethod, path,
                    controllerMethod, 0, 0,
                    serviceName, "Request entered controller"
                ));

                long start = System.currentTimeMillis();

                return mono
                    .doOnSuccess(v -> {
                        long duration = System.currentTimeMillis() - start;
                        log.info("SUCCESS → {} | userId={} | {}ms", controllerMethod, userId, duration);

                        auditPublisher.publish(SecurityAuditEvent.build(
                            AuditEventType.REQUEST_SUCCESS,
                            userId, role, httpMethod, path,
                            controllerMethod, 200, duration,
                            serviceName, "Completed successfully"
                        ));
                    })

                    .doOnError(AccessDeniedException.class, ex -> {
                        log.warn("DENIED → {} | userId={} role={}", controllerMethod, userId, role);

                        auditPublisher.publish(SecurityAuditEvent.build(
                            AuditEventType.ACCESS_DENIED,
                            userId, role, httpMethod, path,
                            controllerMethod, 403, 0,
                            serviceName, "PreAuthorize denied: " + ex.getMessage()
                        ));
                    })

                    .doOnError(ex -> !(ex instanceof AccessDeniedException), ex -> {
                        log.error("ERROR → {} | userId={} | {}", controllerMethod, userId, ex.getMessage(), ex);

                        auditPublisher.publish(SecurityAuditEvent.build(
                            AuditEventType.REQUEST_ERROR,
                            userId, role, httpMethod, path,
                            controllerMethod, 500, 0,
                            serviceName, "Error: " + ex.getMessage()
                        ));
                    })

                    .doFinally(signal -> MDC.clear());
            });
        }

        return result;
    }
}