package com.demo.orderProcessingService.validation;

import com.demo.orderProcessingService.domain.OrderEntity;
import org.springframework.stereotype.Component;

@Component("tenantA")
public class TenantAValidator implements TenantOrderValidator {
  @Override
  public boolean validate(OrderEntity order) {
    return order.getAmount() > 100;
  }
}
