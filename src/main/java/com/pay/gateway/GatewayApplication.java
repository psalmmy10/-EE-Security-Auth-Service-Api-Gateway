package com.pay.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Edge gateway for Banffpay services. Reactive (Netty): one event loop per core handles thousands of
 * concurrent proxied connections, so virtual threads are not needed here.
 */
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
