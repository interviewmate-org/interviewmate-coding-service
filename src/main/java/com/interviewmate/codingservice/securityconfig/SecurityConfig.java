package com.interviewmate.codingservice.securityconfig;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import com.interviewmate.codingservice.securityconfig.filter.GatewayAuthFilter;
import com.interviewmate.codingservice.util.ErrorResponseWriter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity   
@RequiredArgsConstructor
public class SecurityConfig {

    private final ErrorResponseWriter errorWriter;
    private final GatewayAuthFilter authFilter;

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

            // Clean JSON error responses
            .exceptionHandling(ex -> ex
                .accessDeniedHandler((exchange, e) ->
                    errorWriter.write(exchange.getResponse(),
                        HttpStatus.FORBIDDEN, "Access denied",
                        exchange.getRequest().getURI().getPath()))
                .authenticationEntryPoint((exchange, e) ->
                    errorWriter.write(exchange.getResponse(),
                        HttpStatus.UNAUTHORIZED, "Unauthorized",
                        exchange.getRequest().getURI().getPath()))
            )
            .addFilterAt(authFilter, SecurityWebFiltersOrder.AUTHENTICATION)

            .build();
    }

}