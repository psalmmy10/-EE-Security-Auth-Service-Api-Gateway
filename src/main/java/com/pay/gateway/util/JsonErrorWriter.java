package com.pay.gateway.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pay.gateway.constant.GatewayConstants;
import com.pay.gateway.constant.enums.GatewayErrorCode;
import com.pay.gateway.dto.res.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

/** Writes every gateway-generated error in the standard JSON envelope (never an empty body). */
@Component
@RequiredArgsConstructor
public class JsonErrorWriter {

    private final ObjectMapper objectMapper;

    public Mono<Void> write(ServerWebExchange exchange, GatewayErrorCode code) {
        return write(exchange, code, Map.of());
    }

    public Mono<Void> write(ServerWebExchange exchange, GatewayErrorCode code, Map<String, String> extraHeaders) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.empty();
        }
        response.setStatusCode(code.getStatus());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        extraHeaders.forEach((k, v) -> response.getHeaders().set(k, v));

        String correlationId = exchange.getAttribute(GatewayConstants.ATTR_CORRELATION_ID);
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(ApiResponse.failure(code, correlationId));
        } catch (JsonProcessingException ex) {
            body = ("{\"success\":false,\"code\":\"" + code.getCode() + "\"}").getBytes();
        }
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }
}
