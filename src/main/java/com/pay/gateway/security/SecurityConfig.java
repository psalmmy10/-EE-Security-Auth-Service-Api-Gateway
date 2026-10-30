package com.pay.gateway.security;

import com.pay.gateway.config.properties.GatewayProperties;
import com.pay.gateway.constant.GatewayConstants;
import com.pay.gateway.constant.enums.GatewayErrorCode;
import com.pay.gateway.util.JsonErrorWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Edge security. The gateway PRE-validates JWTs (signature via the auth service's /oauth2/jwks, expiry,
 * issuer, audience) so invalid traffic never reaches a service. Each service still validates the token
 * itself — including the session-revocation check, which only the auth service can do. Defence in depth.
 */
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String AUTH = "/api/v1/auth";

    private final GatewayProperties properties;
    private final JsonErrorWriter errorWriter;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, ReactiveJwtDecoder jwtDecoder) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)                 // bearer tokens, no cookies
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())   // stateless
                .cors(Customizer.withDefaults())
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .frameOptions(frame -> frame.mode(XFrameOptionsServerHttpHeadersWriter.Mode.DENY))
                        .referrerPolicy(ref -> ref.policy(ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.NO_REFERRER))
                        .hsts(hsts -> hsts.includeSubdomains(true).maxAge(Duration.ofDays(365))))
                .authorizeExchange(ex -> ex
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers("/actuator/health/**", "/actuator/health", "/actuator/prometheus").permitAll()
                        .pathMatchers("/dev/**", "/actuator/**").denyAll()                // never exposed publicly
                        .pathMatchers(HttpMethod.POST, AUTH + "/signup", AUTH + "/login", AUTH + "/refresh").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/crypto/public-key", "/oauth2/jwks").permitAll()
                        .pathMatchers("/fallback/**").permitAll()
                        .pathMatchers("/api/v1/admin/**").hasAuthority("ROLE_ADMIN")
                        .pathMatchers("/api/v1/**").authenticated()
                        .anyExchange().denyAll())                                       // unknown paths: closed by default
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtDecoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()));
        return http.build();
    }

    /**
     * Keys come from the auth service's JWKS and are cached; an unknown "kid" (after key rotation)
     * triggers a refetch automatically. RS256 only — blocks algorithm-confusion attacks.
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
                .withJwkSetUri(properties.authServiceUri() + "/oauth2/jwks")
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<List<String>>(
                JwtClaimNames.AUD, aud -> aud != null && aud.contains(properties.jwt().audience()));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.jwt().issuer()), audience));
        return decoder;
    }

    private ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(GatewayConstants.CLAIM_AUTHORITIES);
        authorities.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }

    private ServerAuthenticationEntryPoint authenticationEntryPoint() {
        return (exchange, ex) -> errorWriter.write(exchange,
                ex instanceof InvalidBearerTokenException ? GatewayErrorCode.INVALID_TOKEN : GatewayErrorCode.UNAUTHORIZED,
                Map.of(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
    }

    private ServerAccessDeniedHandler accessDeniedHandler() {
        return (exchange, ex) -> errorWriter.write(exchange, GatewayErrorCode.ACCESS_DENIED);
    }

    /** CORS is answered at the edge (including preflight). Duplicate headers from services are removed in RouteConfig. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.cors().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE,
                GatewayConstants.HEADER_E2E_KEY, GatewayConstants.HEADER_E2E_KEY_ID,
                GatewayConstants.HEADER_E2E_TIMESTAMP, GatewayConstants.HEADER_E2E_NONCE,
                GatewayConstants.HEADER_CORRELATION_ID));
        config.setExposedHeaders(List.of(
                GatewayConstants.HEADER_E2E_ENCRYPTED, GatewayConstants.HEADER_CORRELATION_ID, HttpHeaders.RETRY_AFTER));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
