package com.demo.orderProcessingService.controller;

import com.demo.orderProcessingService.commands.CreateOrderCommand;
import com.demo.orderProcessingService.commands.OrderCommandHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class CommandController {
    private final OrderCommandHandler handler;

    public CommandController(OrderCommandHandler handler) {
        this.handler = handler;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateOrderCommand cmd) {
        String id = handler.handle(cmd);
        return ResponseEntity.accepted().body("{\"orderId\":\"" + id + "\"}");
    }
}