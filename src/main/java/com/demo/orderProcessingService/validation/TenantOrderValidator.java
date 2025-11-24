package com.demo.orderProcessingService.validation;

import com.demo.orderProcessingService.domain.OrderEntity;

public interface TenantOrderValidator {
  boolean validate(OrderEntity order);

  String tenantId();
}
