package com.example.paymentservice.service;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Map<String, String> processPayment(Map<String, String> paymentRequest, String username) {
        log.debug("Processing payment for user: {}, amount: {}", username, paymentRequest.get("amount"));

        Payment payment = new Payment(paymentRequest.get("amount"), username, "SUCCESS");
        paymentRepository.save(payment);
        log.info("Payment saved to DB with id: {} for user: {}", payment.getId(), username);

        return Map.of("message", "Payment successful");
    }
}
