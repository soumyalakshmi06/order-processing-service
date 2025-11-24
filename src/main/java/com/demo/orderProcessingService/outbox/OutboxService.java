package com.demo.orderProcessingService.outbox;

import com.demo.orderProcessingService.domain.OrderEntity;
import com.demo.orderProcessingService.repository.OrderRepository;
import com.demo.orderProcessingService.validation.TenantOrderValidator;
import com.demo.orderProcessingService.validation.ValidatorRegistry;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxService {

  private final OutboxRepository outboxRepository;
  private final OrderRepository orderRepository;
  private final ValidatorRegistry validatorRegistry;

  public OutboxService(
      OutboxRepository outboxRepository,
      OrderRepository orderRepository,
      ValidatorRegistry validatorRegistry) {
    this.outboxRepository = outboxRepository;
    this.orderRepository = orderRepository;
    this.validatorRegistry = validatorRegistry;
  }

  /**
   * Attempt to claim event using a compare-and-set (must run inside a transaction). Returns true if
   * claimed.
   */
  @Transactional
  public boolean tryClaimEvent(Long eventId) {
    int updated =
        outboxRepository.compareAndSetStatus(
            eventId,
            OutboxEvent.Status.IN_PROGRESS,
            Instant.now(),
            Instant.now(),
            OutboxEvent.Status.PENDING);
    return updated > 0;
  }

  /**
   * Process the event in a new transaction so that the claim commit is separated from processing.
   * That helps ensure the event's IN_PROGRESS marker persists.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void processEvent(Long eventId) {
    Optional<OutboxEvent> opt = outboxRepository.findById(eventId);
    if (opt.isEmpty()) return;
    OutboxEvent event = opt.get();
    try {
      String payload = event.getPayload();
      String orderId = extractOrderId(payload);
      OrderEntity order = orderRepository.findById(orderId).orElse(null);
      if (order == null) {
        // nothing to do
        event.setStatus(OutboxEvent.Status.PROCESSED);
        event.setUpdatedAt(Instant.now());
        outboxRepository.save(event);
        return;
      }

      TenantOrderValidator validator = validatorRegistry.get(order.getTenantId());
      boolean ok = validator.validate(order);

      if (ok) {
        order.setStatus(OrderEntity.OrderStatus.PROCESSED);
      } else {
        order.setStatus(OrderEntity.OrderStatus.FAILED);
      }
      orderRepository.save(order);

      event.setStatus(OutboxEvent.Status.PROCESSED);
      event.setUpdatedAt(Instant.now());
      outboxRepository.save(event);
    } catch (Exception ex) {
      event.setStatus(OutboxEvent.Status.FAILED);
      event.setUpdatedAt(Instant.now());
      outboxRepository.save(event);
    }
  }

  private String extractOrderId(String payload) {
    int i = payload.indexOf("\"orderId\"");
    if (i < 0) return null;
    int colon = payload.indexOf(":", i);
    int q1 = payload.indexOf('"', colon);
    int q2 = payload.indexOf('"', q1 + 1);
    if (q1 < 0 || q2 < 0) return null;
    return payload.substring(q1 + 1, q2);
  }
}
