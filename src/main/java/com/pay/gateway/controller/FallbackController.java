package com.pay.gateway.controller;

import com.pay.gateway.constant.GatewayConstants;
import com.pay.gateway.constant.enums.GatewayErrorCode;
import com.pay.gateway.dto.res.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import java.util.concurrent.TimeoutException;

/**
 * Circuit-breaker fallback: when a service is down, slow, or its circuit is open, clients get a fast,
 * clean JSON error instead of hanging connections — and the struggling service gets time to recover.
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/{service}")
    public ResponseEntity<ApiResponse> fallback(@PathVariable String service, ServerWebExchange exchange) {
        Throwable cause = exchange.getAttribute(ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);
        GatewayErrorCode code = cause instanceof TimeoutException
                ? GatewayErrorCode.GATEWAY_TIMEOUT
                : GatewayErrorCode.SERVICE_UNAVAILABLE;
        log.warn("Fallback for {}: {} ({})", service, code.getCode(), cause == null ? "circuit open" : cause.getClass().getSimpleName());
        String correlationId = exchange.getAttribute(GatewayConstants.ATTR_CORRELATION_ID);
        return ResponseEntity.status(code.getStatus())
                .header(HttpHeaders.RETRY_AFTER, "5")
                .body(ApiResponse.failure(code, correlationId));
    }
}
