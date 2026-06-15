package com.example.orderservice.controller;

import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> createOrder(
            @RequestBody OrderRequest orderRequest,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Create order request from user: {}, item: {}", username, orderRequest.item());

        Map<String, String> result = orderService.createOrder(orderRequest, authHeader, username);
        log.info("Order created successfully for user: {}", username);

        return ResponseEntity.ok(result);
    }
}
