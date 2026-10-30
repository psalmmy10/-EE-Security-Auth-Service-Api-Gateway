package com.pay.gateway.exception;

import com.pay.gateway.constant.enums.GatewayErrorCode;
import com.pay.gateway.util.JsonErrorWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

/**
 * Last-resort handler: turns any unhandled error into the standard JSON envelope with a GW-* code.
 * Stack traces and upstream details go to the log only.
 */
@Slf4j
@Component
@Order(-2)   // before Spring Boot's default handler (-1)
@RequiredArgsConstructor
public class GatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private final JsonErrorWriter errorWriter;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        GatewayErrorCode code = map(ex);
        if (code.getStatus().is5xxServerError()) {
            log.error("Gateway error {} on {}", code.getCode(), exchange.getRequest().getPath().value(), ex);
        } else {
            log.warn("Gateway rejected {} on {}: {}", code.getCode(), exchange.getRequest().getPath().value(), ex.getMessage());
        }
        return errorWriter.write(exchange, code);
    }

    private static GatewayErrorCode map(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            HttpStatus status = HttpStatus.resolve(rse.getStatusCode().value());
            if (status != null) {
                return switch (status) {
                    case NOT_FOUND -> GatewayErrorCode.NOT_FOUND;
                    case METHOD_NOT_ALLOWED -> GatewayErrorCode.METHOD_NOT_ALLOWED;
                    case PAYLOAD_TOO_LARGE -> GatewayErrorCode.PAYLOAD_TOO_LARGE;
                    case TOO_MANY_REQUESTS -> GatewayErrorCode.TOO_MANY_REQUESTS;
                    case GATEWAY_TIMEOUT -> GatewayErrorCode.GATEWAY_TIMEOUT;
                    case SERVICE_UNAVAILABLE -> GatewayErrorCode.SERVICE_UNAVAILABLE;
                    case BAD_GATEWAY -> GatewayErrorCode.BAD_GATEWAY;
                    default -> status.is4xxClientError() ? GatewayErrorCode.NOT_FOUND : GatewayErrorCode.INTERNAL_ERROR;
                };
            }
        }
        if (ex instanceof TimeoutException) {
            return GatewayErrorCode.GATEWAY_TIMEOUT;
        }
        if (ex instanceof ConnectException || ex.getCause() instanceof ConnectException) {
            return GatewayErrorCode.BAD_GATEWAY;
        }
        return GatewayErrorCode.INTERNAL_ERROR;
    }
}
