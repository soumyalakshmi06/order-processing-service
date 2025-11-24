package com.demo.orderProcessingService.outbox;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxWorker {

  private final OutboxProcessor processor;

  public OutboxWorker(OutboxProcessor processor) {
    this.processor = processor;
  }

  @Scheduled(fixedDelayString = "${outbox.worker.poll-interval-ms:3000}")
  public void poll() {
    processor.retryStuck();
    processor.processPending();
  }
}
