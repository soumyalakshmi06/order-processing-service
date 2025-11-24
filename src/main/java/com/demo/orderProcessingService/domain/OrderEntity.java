package com.demo.orderProcessingService.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity {
  @Id private String id;

  @Column(nullable = false)
  private String tenantId;

  @Column(nullable = false)
  private double amount;

  @Column(nullable = false)
  private int quantity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status;

  public enum OrderStatus {
    PENDING,
    PROCESSED,
    FAILED
  }
}
