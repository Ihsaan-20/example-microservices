package com.example.authservice.service;

import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.LoginResponse;
import com.example.authservice.entity.Token;
import com.example.authservice.repository.TokenRepository;
import com.example.authservice.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final TokenRepository tokenRepository;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;

    public AuthService(TokenRepository tokenRepository, JwtUtil jwtUtil, RestTemplate restTemplate) {
        this.tokenRepository = tokenRepository;
        this.jwtUtil = jwtUtil;
        this.restTemplate = restTemplate;
    }

    public LoginResponse login(LoginRequest request) {
        log.debug("Login attempt for user: {}", request.username());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(
                Map.of("username", request.username(), "password", request.password()), headers);

        ResponseEntity<Map<String, Object>> response;
        try {
            response = restTemplate.exchange(
                    "http://user-service/api/users/validate",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            log.warn("User-service call failed for {}: {}", request.username(), e.getMessage());
            throw new RuntimeException("Invalid credentials");
        }

        if (response.getBody() == null || !Boolean.TRUE.equals(response.getBody().get("valid"))) {
            throw new RuntimeException("Invalid credentials");
        }

        String username = (String) response.getBody().get("username");
        String jwt = jwtUtil.generateToken(username);
        log.debug("JWT generated for user: {}", username);

        Token tokenEntity = new Token(
                jwt, username,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1)
        );
        tokenRepository.save(tokenEntity);
        log.info("Token saved to DB for user: {}", username);

        return new LoginResponse(jwt);
    }

    public boolean validateToken(String token) {
        if (!jwtUtil.isTokenValid(token)) {
            return false;
        }
        return tokenRepository.findByTokenAndRevokedFalse(token).isPresent();
    }
}
