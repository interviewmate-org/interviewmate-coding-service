package com.interviewmate.codingservice.util;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import com.interviewmate.codingservice.dto.responses.ApiErrorResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public Mono<Void> write(ServerHttpResponse response,
                            HttpStatus status,
                            String message,
                            String path) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return Mono.fromCallable(() -> objectMapper.writeValueAsBytes(
                    ApiErrorResponse.of(status, message, path)))
            .flatMap(bytes -> {
                DataBuffer buffer = response.bufferFactory().wrap(bytes);
                return response.writeWith(Mono.just(buffer));
            });
    }

    // Overload — when path isn't available (e.g. filter early exit)
    public Mono<Void> write(ServerHttpResponse response,
                            HttpStatus status,
                            String message) {
        return write(response, status, message, null);
    }
}