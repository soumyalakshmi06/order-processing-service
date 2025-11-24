package com.demo.orderProcessingService.validation;

import static org.junit.jupiter.api.Assertions.*;

import com.demo.orderProcessingService.domain.OrderEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TenantValidatorsTest {

  @Autowired ValidatorRegistry registry;

  @Test
  public void tenantA_validator() {
    TenantOrderValidator v = registry.get("tenantA");
    OrderEntity o = OrderEntity.builder().tenantId("tenantA").amount(150.0).quantity(1).build();
    assertTrue(v.validate(o));
    o.setAmount(50.0);
    assertFalse(v.validate(o));
  }

  @Test
  public void tenantB_validator() {
    TenantOrderValidator v = registry.get("tenantB");
    OrderEntity o = OrderEntity.builder().tenantId("tenantB").amount(200.0).quantity(20).build();
    assertTrue(v.validate(o));

    o.setQuantity(5);
    assertFalse(v.validate(o));
  }
}
