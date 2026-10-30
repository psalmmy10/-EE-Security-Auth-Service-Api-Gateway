package com.pay.gateway.config;

import com.pay.gateway.exception.StartupConfigurationException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Same rules as the auth service: exactly one of dev/staging/prod, and staging/prod refuse insecure settings.
 * Registered in META-INF/spring.factories.
 */
public class ProfileGuardEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Set<String> ENVIRONMENTS = Set.of("dev", "staging", "prod");
    private static final Set<String> HARDENED = Set.of("staging", "prod");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication application) {
        List<String> active = Arrays.stream(env.getActiveProfiles()).filter(ENVIRONMENTS::contains).toList();
        if (active.size() != 1) {
            throw new StartupConfigurationException(
                    "Exactly one environment profile must be active. Currently active: " + Arrays.toString(env.getActiveProfiles()),
                    "Set spring.profiles.active=dev locally, or SPRING_PROFILES_ACTIVE=staging|prod on servers.");
        }
        String profile = active.get(0);
        if (!HARDENED.contains(profile)) {
            return;
        }
        require(Boolean.TRUE.equals(env.getProperty("app.gateway.require-https", Boolean.class)),
                profile + " must reject non-HTTPS traffic.", "Set app.gateway.require-https=true.");
        require(env.getProperty("app.gateway.jwt.issuer", "").startsWith("https://"),
                profile + " requires an https:// JWT issuer (must equal the auth service's).", "Set JWT_ISSUER.");
        String origins = env.getProperty("app.gateway.cors.allowed-origins", "");
        require(!origins.isBlank() && !origins.contains("localhost") && !origins.contains("*") && !origins.contains("http://"),
                profile + " requires explicit https:// CORS origins, but they are '" + origins + "'.", "Set CORS_ALLOWED_ORIGINS.");
        require(Boolean.TRUE.equals(env.getProperty("app.gateway.rate-limit.enabled", Boolean.class)),
                profile + " must keep edge rate limiting enabled.", "Set app.gateway.rate-limit.enabled=true.");
        String managementPort = env.getProperty("management.server.port");
        require(managementPort != null && !managementPort.equals(env.getProperty("server.port", "8080")),
                profile + " must serve actuator on a separate internal port.", "Set MANAGEMENT_PORT (e.g. 8091).");
    }

    private static void require(boolean condition, String description, String action) {
        if (!condition) {
            throw new StartupConfigurationException("Refusing to start: " + description, action);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
