package com.interviewmate.codingservice.securityconfig.filter;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.interviewmate.codingservice.constants.AuditEventType;
import com.interviewmate.codingservice.dto.events.SecurityAuditEvent;
import com.interviewmate.codingservice.eventcontroller.AuditEventPublisher;
import com.interviewmate.codingservice.securityconfig.SecurityConstants;
import com.interviewmate.codingservice.util.ErrorResponseWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayAuthFilter implements WebFilter {

    private final ErrorResponseWriter errorWriter;
    private final AuditEventPublisher auditPublisher;

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    public static final String CTX_USER_ID = "userId";
    public static final String CTX_ROLE    = "role";
    public static final String CTX_HTTP_METHOD = "httpMethod"; 
    public static final String CTX_PATH        = "path";   

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        ServerHttpRequest  request  = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String             path     = request.getURI().getPath();
        String             method   = request.getMethod().name();

        String role   = request.getHeaders().getFirst(SecurityConstants.HEADER_ROLE);
        String userId = request.getHeaders().getFirst(SecurityConstants.HEADER_USER_ID);

        //  X-Role header must exist
        if (!StringUtils.hasText(role)) {
            String resolvedUserId = StringUtils.hasText(userId) ? userId.trim() : "unknown";

            log.warn("UNAUTHORIZED → missing X-Role | method={} path={} userId={}",
                method, path, resolvedUserId);

            auditPublisher.publish(SecurityAuditEvent.build(
                AuditEventType.UNAUTHORIZED,
                resolvedUserId, "NONE",
                method, path,
                null, 401, 0,
                serviceName, "Missing X-Role header"
            ));

            return errorWriter.write(response, HttpStatus.UNAUTHORIZED,
                "Missing X-Role header", path);
        }

        // Role must be a known value
        String normalized = role.trim().toUpperCase();
        if (!SecurityConstants.VALID_ROLES.contains(normalized)) {
            String resolvedUserId = StringUtils.hasText(userId) ? userId.trim() : "unknown";

            log.warn("UNAUTHORIZED → invalid role='{}' | method={} path={} userId={}",
                role, method, path, resolvedUserId);

            auditPublisher.publish(SecurityAuditEvent.build(
                AuditEventType.UNAUTHORIZED,
                resolvedUserId, role,
                method, path,
                null, 401, 0,
                serviceName, "Invalid role value: " + role
            ));

            return errorWriter.write(response, HttpStatus.UNAUTHORIZED,
                "Invalid role value", path);
        }

        // Build Authentication and pass downstream
        String principal = StringUtils.hasText(userId) ? userId.trim() : "unknown";

        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(
                principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + normalized))
            );

        log.debug("AUTH SET → method={} path={} userId={} role={}",
            method, path, principal, normalized);

        return chain.filter(exchange)
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
            .contextWrite(ctx -> ctx
                .put(CTX_USER_ID, principal)
                .put(CTX_ROLE,    normalized)
                .put(CTX_HTTP_METHOD, method)
                .put(CTX_PATH, path)
            );
    }
}