package com.demo.orderProcessingService.validation;

import com.demo.orderProcessingService.domain.OrderEntity;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ValidatorRegistry {

  private final Map<String, TenantOrderValidator> validators;

  public ValidatorRegistry(Map<String, TenantOrderValidator> validators) {
    this.validators = validators;
  }

  public TenantOrderValidator get(String tenantId) {
    TenantOrderValidator v = validators.get(tenantId);
    if (v != null) return v;
    return new TenantOrderValidator() {
      @Override
      public boolean validate(OrderEntity order) {
        return false;
      }
    };
  }
}
