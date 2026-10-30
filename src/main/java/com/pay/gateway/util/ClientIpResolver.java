package com.pay.gateway.util;

import com.pay.gateway.config.properties.GatewayProperties;
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Finds the real client IP and protocol without trusting values a client could forge.
 * <p>
 * With N trusted proxies in front (e.g. a load balancer = 1), only the N-th address from the
 * RIGHT of X-Forwarded-For is trusted — that is the one our own load balancer appended.
 * Anything further left was supplied by the client and is ignored.
 */
@Component
public class ClientIpResolver {

    private final int trustedProxyHops;
    private final XForwardedRemoteAddressResolver forwardedResolver;

    public ClientIpResolver(GatewayProperties properties) {
        this.trustedProxyHops = properties.trustedProxyHops();
        this.forwardedResolver = trustedProxyHops > 0
                ? XForwardedRemoteAddressResolver.maxTrustedIndex(trustedProxyHops)
                : null;
    }

    public String clientIp(ServerWebExchange exchange) {
        InetSocketAddress address = forwardedResolver != null
                ? forwardedResolver.resolve(exchange)
                : exchange.getRequest().getRemoteAddress();
        if (address == null) {
            return "unknown";
        }
        return address.getAddress() != null ? address.getAddress().getHostAddress() : address.getHostString();
    }

    /** "https" or "http" as seen by the client: from our load balancer's header, else the connection itself. */
    public String clientProto(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        if (trustedProxyHops > 0) {
            List<String> protos = request.getHeaders().getValuesAsList("X-Forwarded-Proto");
            if (!protos.isEmpty() && StringUtils.hasText(protos.get(protos.size() - 1))) {
                return protos.get(protos.size() - 1).trim().toLowerCase();
            }
        }
        return request.getSslInfo() != null ? "https" : "http";
    }
}
