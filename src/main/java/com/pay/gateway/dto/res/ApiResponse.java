package com.pay.gateway.dto.res;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pay.gateway.constant.enums.GatewayErrorCode;

import java.time.Instant;

/** Same envelope as the auth service, so clients parse gateway and service errors identically. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse(boolean success, String code, String message, String correlationId, Instant timestamp) {

    public static ApiResponse failure(GatewayErrorCode code, String correlationId) {
        return new ApiResponse(false, code.getCode(), code.getMessage(), correlationId, Instant.now());
    }
}
