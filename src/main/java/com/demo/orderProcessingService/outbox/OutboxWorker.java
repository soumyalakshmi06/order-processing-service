package com.demo.orderProcessingService.outbox;

import com.demo.orderProcessingService.domain.OrderEntity;
import com.demo.orderProcessingService.repository.OrderRepository;
import com.demo.orderProcessingService.validation.TenantOrderValidator;
import com.demo.orderProcessingService.validation.ValidatorRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class OutboxWorker {

    private final OutboxRepository outboxRepository;
    private final OrderRepository orderRepository;
    private final ValidatorRegistry validatorRegistry;
    private final int batchSize;
    private final int inProgressTimeoutSeconds;

    public OutboxWorker(OutboxRepository outboxRepository,
                        OrderRepository orderRepository,
                        ValidatorRegistry validatorRegistry,
                        @Value("${outbox.worker.batch-size:10}") int batchSize,
                        @Value("${outbox.worker.in-progress-timeout-seconds:60}") int inProgressTimeoutSeconds) {
        this.outboxRepository = outboxRepository;
        this.orderRepository = orderRepository;
        this.validatorRegistry = validatorRegistry;
        this.batchSize = batchSize;
        this.inProgressTimeoutSeconds = inProgressTimeoutSeconds;
    }

    // run periodically
    @Scheduled(fixedDelayString = "${outbox.worker.poll-interval-ms:3000}")
    public void poll() {
        // 1) retry stuck IN_PROGRESS older than timeout -> mark PENDING
        retryStuckEvents();

        // 2) fetch PENDING events
        List<OutboxEvent> pending = outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxEvent.Status.PENDING);
        if (pending.isEmpty()) return;

        // process up to batchSize
        pending.stream().limit(batchSize).forEach(this::claimAndProcess);
    }

    @Transactional
    protected void claimAndProcess(OutboxEvent event) {
        // optimistic compare-and-set
        int updated = outboxRepository.compareAndSetStatus(event.getId(), OutboxEvent.Status.IN_PROGRESS, Instant.now(), Instant.now(), OutboxEvent.Status.PENDING);
        if (updated == 0) return; // someone else claimed

        // now process (in same tx or separate? we do processing in a new transaction per event)
        processEvent(event.getId());
    }

    // new transaction - actual processing & marking final status
    @Transactional
    public void processEvent(Long id) {
        OutboxEvent event = outboxRepository.findById(id).orElse(null);
        if (event == null) return;
        try {
            // simple in-memory processing: payload contains orderId JSON like {"orderId":"..."}
            String payload = event.getPayload();
            String orderId = extractOrderId(payload);

            OrderEntity order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                // nothing to do - mark PROCESSED
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
            // mark FAILED so that operator can inspect (alternatively retry later)
            event.setStatus(OutboxEvent.Status.FAILED);
            event.setUpdatedAt(Instant.now());
            outboxRepository.save(event);
        }
    }

    private String extractOrderId(String payload) {
        // naive extraction for sample payload: {"orderId":"..."}
        int i = payload.indexOf("\"orderId\"");
        if (i < 0) return null;
        int colon = payload.indexOf(":", i);
        int q1 = payload.indexOf('"', colon);
        int q2 = payload.indexOf('"', q1 + 1);
        return payload.substring(q1 + 1, q2);
    }

    @Transactional
    protected void retryStuckEvents() {
        List<OutboxEvent> inProgress = outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxEvent.Status.IN_PROGRESS);
        Instant now = Instant.now();
        for (OutboxEvent e : inProgress) {
            if (e.getProcessingStartedAt() == null) continue;
            if (now.isAfter(e.getProcessingStartedAt().plusSeconds(inProgressTimeoutSeconds))) {
                // mark back to PENDING to retry
                e.setStatus(OutboxEvent.Status.PENDING);
                e.setUpdatedAt(Instant.now());
                e.setProcessingStartedAt(null);
                outboxRepository.save(e);
            }
        }
    }
}