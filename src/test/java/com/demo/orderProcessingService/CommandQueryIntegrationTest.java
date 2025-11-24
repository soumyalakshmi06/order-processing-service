package com.demo.orderProcessingService;

import static org.junit.jupiter.api.Assertions.*;

import com.demo.orderProcessingService.domain.OrderEntity;
import com.demo.orderProcessingService.outbox.OutboxRepository;
import com.demo.orderProcessingService.outbox.OutboxService;
import com.demo.orderProcessingService.repository.OrderRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CommandQueryIntegrationTest {

  @Autowired TestRestTemplate restTemplate;

  @Autowired OutboxRepository outboxRepository;

  @Autowired OrderRepository orderRepository;

  @Autowired OutboxService outboxService;

  @Test
  public void createOrder_and_process_outbox() throws InterruptedException {
    var payload = Map.of("tenantId", "tenantA", "amount", 150.0, "quantity", 2);

    ResponseEntity<String> post =
        restTemplate.postForEntity("/api/v1/orders", payload, String.class);
    assertEquals(HttpStatus.ACCEPTED, post.getStatusCode());
    assertTrue(post.getBody().contains("orderId"));

    String body = post.getBody();
    String id = body.replaceAll(".*\"orderId\"\\s*:\\s*\"([^\"]+)\".*", "$1");

    Thread.sleep(200);
    var evt =
        outboxRepository
            .findByStatusOrderByCreatedAtAsc(
                com.demo.orderProcessingService.outbox.OutboxEvent.Status.PENDING)
            .stream()
            .findFirst();
    assertTrue(evt.isPresent());

    boolean claimed = outboxService.tryClaimEvent(evt.get().getId());
    assertTrue(claimed);
    outboxService.processEvent(evt.get().getId());

    var updated = restTemplate.getForEntity("/api/v1/orders/" + id, OrderEntity.class);
    assertEquals(HttpStatus.OK, updated.getStatusCode());
    assertNotNull(updated.getBody());
    assertEquals(OrderEntity.OrderStatus.PROCESSED, updated.getBody().getStatus());
  }
}
