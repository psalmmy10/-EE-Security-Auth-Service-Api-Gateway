package com.pay.gateway.config;

import com.pay.gateway.exception.StartupConfigurationException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class StartupFailureAnalyzer extends AbstractFailureAnalyzer<StartupConfigurationException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, StartupConfigurationException cause) {
        return new FailureAnalysis(cause.getMessage(), cause.getAction(), cause);
    }
}
