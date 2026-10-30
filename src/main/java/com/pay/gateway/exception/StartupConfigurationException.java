package com.pay.gateway.exception;

import lombok.Getter;

/** Configuration problem the operator must fix; printed as a Description / Action report. */
@Getter
public class StartupConfigurationException extends RuntimeException {

    private final String action;

    public StartupConfigurationException(String description, String action) {
        super(description);
        this.action = action;
    }
}
