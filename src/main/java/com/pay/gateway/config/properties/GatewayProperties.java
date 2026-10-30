package com.pay.gateway.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * @param authServiceUri   internal URL of the auth service (never public), e.g. http://auth-service:8080
 * @param maxRequestSize   largest request body accepted at the edge
 * @param trustedProxyHops number of trusted proxies in FRONT of the gateway (load balancer = 1, none = 0).
 *                         Used to find the real client IP in X-Forwarded-For without trusting spoofed values.
 * @param requireHttps     reject requests that did not arrive over HTTPS (staging/prod)
 */
@Validated
@ConfigurationProperties(prefix = "app.gateway")
public record GatewayProperties(
        @NotBlank String authServiceUri,
        @NotNull DataSize maxRequestSize,
        @Min(0) int trustedProxyHops,
        boolean requireHttps,
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Cors cors,
        @Valid @NotNull RateLimit rateLimit) {

    /** Must match the auth service's app.security.jwt.issuer / audience exactly. */
    public record Jwt(@NotBlank String issuer, @NotBlank String audience) {
    }

    public record Cors(@NotEmpty List<String> allowedOrigins) {
    }

    /** Coarse per-client-IP limit for ALL routes. Endpoint-specific limits stay in each service. */
    public record RateLimit(boolean enabled, @NotBlank String store, @Min(1) int limit, @NotNull Duration window) {
    }
}
