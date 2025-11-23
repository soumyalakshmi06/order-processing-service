package com.demo.orderProcessingService.commands;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderCommand {
    private String tenantId;
    private double amount;
    private int quantity;
}