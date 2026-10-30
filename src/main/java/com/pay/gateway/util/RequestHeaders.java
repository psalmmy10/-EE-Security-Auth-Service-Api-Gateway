package com.pay.gateway.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;

import java.util.function.Consumer;

/**
 * Safe way to change request headers in a filter.
 * <p>
 * Once a request has been mutated by an earlier filter, Spring may expose its headers read-only;
 * calling remove()/set() on them throws UnsupportedOperationException. This helper always works
 * on a fresh, writable COPY and attaches it to a decorated request.
 */
public final class RequestHeaders {

    private RequestHeaders() {
    }

    public static ServerWebExchange modify(ServerWebExchange exchange, Consumer<HttpHeaders> changes) {
        HttpHeaders copy = new HttpHeaders();
        copy.addAll(exchange.getRequest().getHeaders());
        changes.accept(copy);
        HttpHeaders result = HttpHeaders.readOnlyHttpHeaders(copy);

        ServerHttpRequest request = new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public HttpHeaders getHeaders() {
                return result;
            }
        };
        return exchange.mutate().request(request).build();
    }
}