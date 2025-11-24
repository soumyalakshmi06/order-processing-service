package com.demo.orderProcessingService.queries;

import com.demo.orderProcessingService.domain.OrderEntity;
import com.demo.orderProcessingService.repository.OrderRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class OrderQueryHandler {
  private final OrderRepository orderRepository;

  public OrderQueryHandler(OrderRepository orderRepository) {
    this.orderRepository = orderRepository;
  }

  public Optional<OrderEntity> getById(String id) {
    return orderRepository.findById(id);
  }
}
