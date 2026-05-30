package com.interviewmate.codingservice.securityconfig;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.server.SecurityWebFilterChain;

import com.interviewmate.codingservice.constants.AuditEventType;
import com.interviewmate.codingservice.dto.events.SecurityAuditEvent;
import com.interviewmate.codingservice.eventcontroller.AuditEventPublisher;
import com.interviewmate.codingservice.securityconfig.filter.GatewayAuthFilter;
import com.interviewmate.codingservice.util.ErrorResponseWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ErrorResponseWriter  errorWriter;
    private final GatewayAuthFilter    authFilter;
    private final AuditEventPublisher  auditPublisher;

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .logout(ServerHttpSecurity.LogoutSpec::disable)

            .authorizeExchange(auth -> auth
                .pathMatchers("/actuator/health").permitAll()
                .pathMatchers("/admin/**").hasRole("ADMIN")
                .pathMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                .anyExchange().authenticated()
            )

            .exceptionHandling(ex -> ex
                .accessDeniedHandler((exchange, e) -> {
                    String path       = exchange.getRequest().getURI().getPath();
                    String httpMethod = exchange.getRequest().getMethod().name();

                    return ReactiveSecurityContextHolder.getContext()
                        .map(SecurityContext::getAuthentication)
                        .doOnNext(auth -> {
                            String userId = auth.getName();
                            String role   = extractRole(auth);

                            log.warn("DENIED (url-level) → {} {} | userId={} role={}",
                                httpMethod, path, userId, role);

                            auditPublisher.publish(SecurityAuditEvent.build(
                                AuditEventType.ACCESS_DENIED,
                                userId, role,
                                httpMethod, path,
                                null, 403, 0,
                                serviceName, "URL-level access denied"
                            ));
                        })
                        .then(errorWriter.write(
                            exchange.getResponse(),
                            HttpStatus.FORBIDDEN,
                            "Access denied", path
                        ));
                })

                .authenticationEntryPoint((exchange, e) -> {
                    String path       = exchange.getRequest().getURI().getPath();
                    String httpMethod = exchange.getRequest().getMethod().name();

                    log.warn("UNAUTHORIZED → {} {} | {}",
                        httpMethod, path, e.getMessage());

                    return errorWriter.write(
                        exchange.getResponse(),
                        HttpStatus.UNAUTHORIZED,
                        "Unauthorized", path
                    );
                })
            )

            .addFilterAt(authFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }

    private String extractRole(Authentication auth) {
        return auth.getAuthorities().stream()
            .findFirst()
            .map(GrantedAuthority::getAuthority)
            .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
            .orElse("UNKNOWN");
    }
}