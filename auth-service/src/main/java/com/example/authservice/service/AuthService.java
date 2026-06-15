package com.example.authservice.service;

import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.LoginResponse;
import com.example.authservice.entity.Token;
import com.example.authservice.entity.User;
import com.example.authservice.repository.TokenRepository;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, TokenRepository tokenRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest request) {
        log.debug("Login attempt for user: {}", request.username());

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    log.warn("User not found: {}", request.username());
                    return new RuntimeException("Invalid credentials");
                });

        if (!user.getPassword().equals(request.password())) {
            log.warn("Invalid password for user: {}", request.username());
            throw new RuntimeException("Invalid credentials");
        }

        String jwt = jwtUtil.generateToken(user.getUsername());
        log.debug("JWT generated for user: {}", user.getUsername());

        Token tokenEntity = new Token(
                jwt, user.getUsername(),
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1)
        );
        tokenRepository.save(tokenEntity);
        log.info("Token saved to DB for user: {}", user.getUsername());

        return new LoginResponse(jwt);
    }

    public boolean validateToken(String token) {
        if (!jwtUtil.isTokenValid(token)) {
            return false;
        }
        return tokenRepository.findByTokenAndRevokedFalse(token).isPresent();
    }
}
