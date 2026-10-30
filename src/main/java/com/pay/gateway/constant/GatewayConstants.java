package com.pay.gateway.constant;

import java.util.List;

public final class GatewayConstants {

    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    public static final String HEADER_E2E_KEY = "X-E2E-Key";
    public static final String HEADER_E2E_KEY_ID = "X-E2E-Key-Id";
    public static final String HEADER_E2E_TIMESTAMP = "X-E2E-Timestamp";
    public static final String HEADER_E2E_NONCE = "X-E2E-Nonce";
    public static final String HEADER_E2E_ENCRYPTED = "X-E2E-Encrypted";

    /** Exchange attributes shared between filters. */
    public static final String ATTR_CORRELATION_ID = "gateway.correlationId";
    public static final String ATTR_CLIENT_IP = "gateway.clientIp";
    public static final String ATTR_CLIENT_PROTO = "gateway.clientProto";

    public static final String CLAIM_AUTHORITIES = "authorities";

    public static final String AUTH_SERVICE_ROUTE = "auth-service";
    public static final String AUTH_CIRCUIT_BREAKER = "authService";

    /**
     * Client-supplied headers that are always removed before forwarding. Downstream services trust the
     * forwarded-* headers the gateway sets itself, so a client must never be able to inject them.
     */
    public static final List<String> UNTRUSTED_INBOUND_HEADERS = List.of(
            "Forwarded", "X-Forwarded-For", "X-Forwarded-Host", "X-Forwarded-Port",
            "X-Forwarded-Proto", "X-Forwarded-Prefix", "X-Real-IP", "X-Original-URL", "X-Rewrite-URL");

    private GatewayConstants() {
    }
}
