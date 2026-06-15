package com.example.orderservice.repository;

import com.example.orderservice.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByTokenAndRevokedFalse(String token);
}
