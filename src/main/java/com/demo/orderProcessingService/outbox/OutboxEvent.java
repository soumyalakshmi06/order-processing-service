package com.demo.orderProcessingService.outbox;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // simple type + payload
  private String eventType;

  @Column(columnDefinition = "CLOB")
  private String payload;

  @Enumerated(EnumType.STRING)
  private Status status;

  private Instant createdAt;
  private Instant updatedAt;
  private Instant processingStartedAt;

  public enum Status {
    PENDING,
    IN_PROGRESS,
    PROCESSED,
    FAILED
  }
}
