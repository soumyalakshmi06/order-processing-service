package com.demo.orderProcessingService.validation;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ValidatorRegistry {
    private final Map<String, TenantOrderValidator> validators;

    public ValidatorRegistry(Map<String, TenantOrderValidator> validators) {
        this.validators = validators;
    }

    public TenantOrderValidator get(String tenantId) {
        // try exact match by bean name (we named beans "tenantA"/"tenantB")
        TenantOrderValidator v = validators.get(tenantId);
        if (v != null) return v;
        // else default validator that always fails (safe)
        return order -> false;
    }
}
