package com.demo.orderProcessingService.validation;

import com.demo.orderProcessingService.domain.OrderEntity;
import org.springframework.stereotype.Component;

@Component("tenantB")
public class TenantBValidator implements TenantOrderValidator {
    @Override
    public boolean validate(OrderEntity order) {
        return order.getAmount() > 100.0 && order.getQuantity() > 10;
    }

}