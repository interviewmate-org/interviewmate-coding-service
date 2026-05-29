package com.interviewmate.codingservice.securityconfig.filter;

import java.util.List;

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

import com.interviewmate.codingservice.securityconfig.SecurityConstants;
import com.interviewmate.codingservice.util.ErrorResponseWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayAuthFilter implements WebFilter {

    private final ErrorResponseWriter errorWriter;

    public static final String CTX_USER_ID = "userId";
    public static final String CTX_ROLE    = "role";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        ServerHttpRequest request  = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getURI().getPath();

        String role   = request.getHeaders().getFirst(SecurityConstants.HEADER_ROLE);
        String userId = request.getHeaders().getFirst(SecurityConstants.HEADER_USER_ID);

        //  1. Header must exist 
        if (!StringUtils.hasText(role)) {
            return errorWriter.write(response, HttpStatus.UNAUTHORIZED,
                "Missing required header: ", path);
        }

        // 2. Role must be a known value
        String normalized = role.trim().toUpperCase();
        if (!SecurityConstants.VALID_ROLES.contains(normalized)) {
            return errorWriter.write(response, HttpStatus.UNAUTHORIZED,
                "Invalid role value", path);
        }

        // 3. Build Authentication 
         String principal = StringUtils.hasText(userId) ? userId.trim() : "unknown";

        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(
                principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + normalized))
            );

        return chain.filter(exchange)
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
             .contextWrite(ctx -> ctx
                .put(CTX_USER_ID, principal)
                .put(CTX_ROLE,    normalized)
            );
    }

}