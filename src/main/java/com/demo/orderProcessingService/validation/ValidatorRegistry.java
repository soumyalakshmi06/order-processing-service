package com.demo.orderProcessingService.validation;

import com.demo.orderProcessingService.domain.OrderEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ValidatorRegistry {

  private final Map<String, TenantOrderValidator> registry = new HashMap<>();

  public ValidatorRegistry(List<TenantOrderValidator> validators) {
    for (TenantOrderValidator v : validators) {
      registry.put(v.tenantId(), v);
      System.out.println("Registered validator for tenant: " + v.tenantId());
    }
  }

  public TenantOrderValidator get(String tenantId) {
    return registry.getOrDefault(
        tenantId,
        new TenantOrderValidator() {

          @Override
          public boolean validate(OrderEntity order) {
            return false; // safely fail if tenant not recognized
          }

          @Override
          public String tenantId() {
            return "unknown"; // fallback tenant ID
          }
        });
  }
}
