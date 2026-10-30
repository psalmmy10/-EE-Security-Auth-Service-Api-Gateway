package com.pay.gateway.constant.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Errors produced by the gateway itself. Auth/permission errors reuse the auth service's AUTH-* codes
 * so front ends handle a 401/403 the same way whichever layer rejected it.
 */
@Getter
public enum GatewayErrorCode {

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH-4012", "Authentication is required"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-4011", "Invalid or expired token"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH-4030", "You do not have permission to perform this action"),
    HTTPS_REQUIRED(HttpStatus.FORBIDDEN, "GW-4033", "HTTPS is required"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "GW-4040", "Resource not found"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "GW-4050", "Method not allowed"),
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "GW-4130", "Request body is too large"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "GW-4290", "Too many requests. Please slow down and try again later"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GW-5000", "An unexpected error occurred"),
    BAD_GATEWAY(HttpStatus.BAD_GATEWAY, "GW-5020", "Upstream service is unavailable"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "GW-5030", "Service temporarily unavailable. Please retry shortly"),
    GATEWAY_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "GW-5040", "Upstream service timed out");

    private final HttpStatus status;
    private final String code;
    private final String message;

    GatewayErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
