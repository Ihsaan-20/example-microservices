package com.example.orderservice.controller;

import com.example.orderservice.dto.LoginRequest;
import com.example.orderservice.dto.LoginResponse;
import com.example.orderservice.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        log.info("Login request for user: {}", request.username());
        try {
            LoginResponse response = authService.login(request);
            log.info("Login successful for user: {}", request.username());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("Login failed for user: {} - {}", request.username(), e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }
}
