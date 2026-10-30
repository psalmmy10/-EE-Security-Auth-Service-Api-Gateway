package com.pay.gateway.util;

import com.pay.gateway.config.properties.GatewayProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.util.unit.DataSize;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private static GatewayProperties props(int hops) {
        return new GatewayProperties("http://auth:8080", DataSize.ofKilobytes(64), hops, false,
                new GatewayProperties.Jwt("https://auth.test", "api"),
                new GatewayProperties.Cors(List.of("https://app.test")),
                new GatewayProperties.RateLimit(true, "memory", 10, Duration.ofMinutes(1)));
    }

    private static MockServerWebExchange exchange(String xff) {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/users/me")
                .remoteAddress(new InetSocketAddress("10.0.0.5", 44321))
                .header("X-Forwarded-For", xff));
    }

    @Test
    void behindOneLoadBalancerTrustsOnlyTheRightmostAddress() {
        // client forged "6.6.6.6"; our load balancer appended the real client "41.58.1.2"
        assertThat(new ClientIpResolver(props(1)).clientIp(exchange("6.6.6.6, 41.58.1.2"))).isEqualTo("41.58.1.2");
    }

    @Test
    void withoutProxiesIgnoresForwardedHeadersEntirely() {
        assertThat(new ClientIpResolver(props(0)).clientIp(exchange("6.6.6.6"))).isEqualTo("10.0.0.5");
    }
}
