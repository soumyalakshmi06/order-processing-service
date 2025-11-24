package com.demo.orderProcessingService.outbox;

import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxWorker {

  private final OutboxRepository outboxRepository;
  private final OutboxService outboxService;
  private final int batchSize;

  public OutboxWorker(
      OutboxRepository outboxRepository,
      OutboxService outboxService,
      @org.springframework.beans.factory.annotation.Value("${outbox.worker.batch-size:10}")
          int batchSize) {
    this.outboxRepository = outboxRepository;
    this.outboxService = outboxService;
    this.batchSize = batchSize;
  }

  @Scheduled(fixedDelayString = "${outbox.worker.poll-interval-ms:3000}")
  public void poll() {
    // 1) pick pending events
    List<OutboxEvent> pending =
        outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxEvent.Status.PENDING);
    if (pending.isEmpty()) return;

    pending.stream().limit(batchSize).forEach(this::claimAndProcessSafe);
  }

  private void claimAndProcessSafe(OutboxEvent e) {
    try {
      boolean claimed = outboxService.tryClaimEvent(e.getId());
      if (!claimed) return;
      outboxService.processEvent(e.getId());
    } catch (Exception ex) {
      System.err.println("Failed processing outbox event " + e.getId() + ": " + ex.getMessage());
      ex.printStackTrace();
    }
  }
}
