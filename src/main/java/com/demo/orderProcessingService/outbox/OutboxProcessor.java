package com.demo.orderProcessingService.outbox;

import com.demo.orderProcessingService.domain.OrderEntity;
import com.demo.orderProcessingService.repository.OrderRepository;
import com.demo.orderProcessingService.validation.ValidatorRegistry;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxProcessor {

  private final OutboxRepository outboxRepository;
  private final OrderRepository orderRepository;
  private final ValidatorRegistry validatorRegistry;
  private final int batchSize;
  private final int timeoutSeconds;

  public OutboxProcessor(
      OutboxRepository outboxRepository,
      OrderRepository orderRepository,
      ValidatorRegistry validatorRegistry,
      @org.springframework.beans.factory.annotation.Value("${outbox.worker.batch-size:10}")
          int batchSize,
      @org.springframework.beans.factory.annotation.Value(
              "${outbox.worker.in-progress-timeout-seconds:60}")
          int timeoutSeconds) {
    this.outboxRepository = outboxRepository;
    this.orderRepository = orderRepository;
    this.validatorRegistry = validatorRegistry;
    this.batchSize = batchSize;
    this.timeoutSeconds = timeoutSeconds;
  }

  @Transactional
  public void processPending() {

    List<OutboxEvent> pending =
        outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxEvent.Status.PENDING);

    pending.stream().limit(batchSize).forEach(this::claim);
  }

  @Transactional
  public void claim(OutboxEvent event) {

    int updated =
        outboxRepository.compareAndSetStatus(
            event.getId(),
            OutboxEvent.Status.IN_PROGRESS,
            Instant.now(),
            Instant.now(),
            OutboxEvent.Status.PENDING);

    if (updated > 0) {
      processEvent(event.getId());
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void processEvent(Long eventId) {

    OutboxEvent event = outboxRepository.findById(eventId).orElse(null);
    if (event == null) return;

    try {
      String orderId = extractOrderId(event.getPayload());
      OrderEntity order = orderRepository.findById(orderId).orElse(null);

      if (order == null) {
        event.setStatus(OutboxEvent.Status.PROCESSED);
        event.setUpdatedAt(Instant.now());
        outboxRepository.save(event);
        return;
      }

      boolean ok = validatorRegistry.get(order.getTenantId()).validate(order);

      order.setStatus(ok ? OrderEntity.OrderStatus.PROCESSED : OrderEntity.OrderStatus.FAILED);
      orderRepository.save(order);

      event.setStatus(OutboxEvent.Status.PROCESSED);
      event.setUpdatedAt(Instant.now());
      outboxRepository.save(event);

    } catch (Exception e) {

      event.setStatus(OutboxEvent.Status.FAILED);
      event.setUpdatedAt(Instant.now());
      outboxRepository.save(event);
    }
  }

  @Transactional
  public void retryStuck() {
    List<OutboxEvent> stuck =
        outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxEvent.Status.IN_PROGRESS);

    Instant now = Instant.now();

    for (OutboxEvent e : stuck) {
      if (e.getProcessingStartedAt() == null) continue;

      if (now.isAfter(e.getProcessingStartedAt().plusSeconds(timeoutSeconds))) {
        e.setStatus(OutboxEvent.Status.PENDING);
        e.setProcessingStartedAt(null);
        e.setUpdatedAt(Instant.now());
        outboxRepository.save(e);
      }
    }
  }

  private String extractOrderId(String payload) {
    int key = payload.indexOf("\"orderId\"");
    int colon = payload.indexOf(":", key);
    int q1 = payload.indexOf('"', colon);
    int q2 = payload.indexOf('"', q1 + 1);
    return payload.substring(q1 + 1, q2);
  }
}
