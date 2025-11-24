package com.demo.orderProcessingService.commands;

import com.demo.orderProcessingService.domain.OrderEntity;
import com.demo.orderProcessingService.outbox.OutboxEvent;
import com.demo.orderProcessingService.outbox.OutboxRepository;
import com.demo.orderProcessingService.repository.OrderRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderCommandHandler {
  private final OrderRepository orderRepo;
  private final OutboxRepository outboxRepo;

  public OrderCommandHandler(OrderRepository orderRepo, OutboxRepository outboxRepo) {
    this.orderRepo = orderRepo;
    this.outboxRepo = outboxRepo;
  }

  @Transactional
  public String handle(CreateOrderCommand cmd) {
    String id = UUID.randomUUID().toString();
    OrderEntity order =
        OrderEntity.builder()
            .id(id)
            .tenantId(cmd.getTenantId())
            .amount(cmd.getAmount())
            .quantity(cmd.getQuantity())
            .status(OrderEntity.OrderStatus.PENDING)
            .build();

    orderRepo.save(order);

    OutboxEvent event =
        OutboxEvent.builder()
            .eventType("ORDER_CREATED")
            .payload("{\"orderId\":\"" + id + "\"}")
            .status(OutboxEvent.Status.PENDING)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    outboxRepo.save(event);
    return id;
  }
}
