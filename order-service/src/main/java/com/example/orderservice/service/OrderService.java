package com.example.orderservice.service;

import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.entity.Order;
import com.example.orderservice.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private static final String PAYMENT_SERVICE = "payment-service";

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;

    public OrderService(OrderRepository orderRepository, RestTemplate restTemplate) {
        this.orderRepository = orderRepository;
        this.restTemplate = restTemplate;
    }

    @CircuitBreaker(name = PAYMENT_SERVICE, fallbackMethod = "paymentFallback")
    public Map<String, String> createOrder(OrderRequest orderRequest, String authHeader, String username) {
        log.debug("Creating order for user: {}, item: {}, quantity: {}",
                username, orderRequest.item(), orderRequest.quantity());

        Order order = new Order(orderRequest.item(), orderRequest.quantity(), username, "PENDING");
        orderRepository.save(order);
        log.info("Order saved to DB with id: {} for user: {}", order.getId(), username);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", authHeader);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(
                Map.of("amount", String.valueOf(orderRequest.quantity() * 100)), headers);

        log.debug("Calling payment service via discovery: {}/payments", PAYMENT_SERVICE);
        ResponseEntity<Map> paymentResponse = restTemplate.postForEntity(
                "http://" + PAYMENT_SERVICE + "/payments", request, Map.class);
        log.info("Payment service responded: {}", paymentResponse.getBody());

        order.setStatus("COMPLETED");
        orderRepository.save(order);
        log.debug("Order {} status updated to COMPLETED", order.getId());

        return Map.of(
                "orderId", String.valueOf(order.getId()),
                "order", "Order created for " + orderRequest.item(),
                "payment", paymentResponse.getBody().get("message").toString()
        );
    }

    public Map<String, String> paymentFallback(OrderRequest orderRequest, String authHeader, String username, Throwable t) {
        log.warn("Payment service unavailable. Order saved with PENDING status. Error: {}", t.getMessage());

        Order order = new Order(orderRequest.item(), orderRequest.quantity(), username, "PENDING");
        orderRepository.save(order);
        log.info("Order saved to DB with id: {} in PENDING state (fallback)", order.getId());

        return Map.of(
                "orderId", String.valueOf(order.getId()),
                "order", "Order created for " + orderRequest.item() + " (payment pending)",
                "payment", "Payment service unavailable, order queued for retry"
        );
    }
}
