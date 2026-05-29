package com.interviewmate.codingservice.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import com.interviewmate.codingservice.securityconfig.filter.GatewayAuthFilter;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Aspect
@Component
public class SecurityLoggingAspect {

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object logControllerAccess(ProceedingJoinPoint pjp) throws Throwable {

        String method = pjp.getSignature().toShortString();
        Object result = pjp.proceed();

        // Controller returns Mono — wrap it to read Reactor Context
        if (result instanceof Mono<?> mono) {
            return Mono.deferContextual(ctx -> {

                // Read from Reactor Context — available on any thread
                String userId = ctx.getOrDefault(GatewayAuthFilter.CTX_USER_ID, "unknown");
                String role   = ctx.getOrDefault(GatewayAuthFilter.CTX_ROLE,    "unknown");

                // Populate MDC here — on the  thread
                MDC.put("userId", userId);
                MDC.put("role",   role);

                log.info("ACCESS → {} | userId={} role={}", method, userId, role);

                long start = System.currentTimeMillis();

                return mono
                    .doOnSuccess(v ->
                        log.info("SUCCESS → {} | userId={} | {}ms",
                            method, userId, System.currentTimeMillis() - start))

                    .doOnError(e -> {
                        if (e instanceof AccessDeniedException) {
                            log.warn("DENIED → {} | userId={} role={}", method, userId, role);
                        } else {
                            log.error("ERROR → {} | userId={} | {}",
                                method, userId, e.getMessage());
                        }
                    })
                    .doFinally(signal -> MDC.clear());
            });
        }

        return result;
    }

    @AfterThrowing(pointcut = "@annotation(preAuthorize)", throwing = "ex")
    public void logPreAuthorizeDenial(PreAuthorize preAuthorize, AccessDeniedException ex) {
        log.warn("@PreAuthorize DENIED | rule='{}' | userId={} role={}",
            preAuthorize.value(),
            MDC.get("userId"),
            MDC.get("role"));
    }
}