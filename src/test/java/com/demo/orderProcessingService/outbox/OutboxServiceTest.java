package com.demo.orderProcessingService.outbox;

import static org.junit.jupiter.api.Assertions.*;

import com.demo.orderProcessingService.domain.OrderEntity;
import com.demo.orderProcessingService.repository.OrderRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class OutboxServiceTest {

  @Autowired OutboxRepository outboxRepository;

  @Autowired OrderRepository orderRepository;

  @Autowired OutboxService outboxService;

  @Test
  public void process_pending_event_should_update_order() {
    String id = "test-order-1";
    OrderEntity order =
        OrderEntity.builder()
            .id(id)
            .tenantId("tenantA")
            .amount(200.0)
            .quantity(1)
            .status(OrderEntity.OrderStatus.PENDING)
            .build();
    orderRepository.save(order);

    OutboxEvent e =
        OutboxEvent.builder()
            .eventType("ORDER_CREATED")
            .payload("{\"orderId\":\"" + id + "\"}")
            .status(OutboxEvent.Status.PENDING)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    outboxRepository.save(e);

    boolean claimed = outboxService.tryClaimEvent(e.getId());
    assertTrue(claimed, "should claim event");

    outboxService.processEvent(e.getId());

    OrderEntity updated = orderRepository.findById(id).orElseThrow();
    assertEquals(OrderEntity.OrderStatus.PROCESSED, updated.getStatus());
  }
}
