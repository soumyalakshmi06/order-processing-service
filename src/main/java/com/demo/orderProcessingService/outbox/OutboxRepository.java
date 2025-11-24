package com.demo.orderProcessingService.outbox;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
  List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEvent.Status status);

  @Modifying
  @Query(
      "update OutboxEvent e set e.status = ?2, e.processingStartedAt = ?3, e.updatedAt = ?4 where e.id = ?1 and e.status = ?5")
  int compareAndSetStatus(
      Long id,
      OutboxEvent.Status newStatus,
      java.time.Instant processingStartedAt,
      java.time.Instant updatedAt,
      OutboxEvent.Status expectedStatus);
}
