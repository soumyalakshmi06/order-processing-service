package com.demo.orderProcessingService.controller;

import com.demo.orderProcessingService.queries.OrderQueryHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class QueryController {
    private final OrderQueryHandler queryHandler;

    public QueryController(OrderQueryHandler queryHandler){
        this.queryHandler = queryHandler;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        return queryHandler.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}