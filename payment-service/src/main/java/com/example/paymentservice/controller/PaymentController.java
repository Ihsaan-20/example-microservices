package com.example.paymentservice.controller;

import com.example.paymentservice.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> processPayment(
            @RequestBody Map<String, String> paymentRequest,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Payment request from user: {}, amount: {}", username, paymentRequest.get("amount"));

        Map<String, String> result = paymentService.processPayment(paymentRequest, username);
        log.info("Payment successful for user: {}", username);

        return ResponseEntity.ok(result);
    }
}
