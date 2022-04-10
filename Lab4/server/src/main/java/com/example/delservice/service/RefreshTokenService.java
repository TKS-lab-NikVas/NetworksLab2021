package com.example.delservice.service;

import com.example.delservice.model.RefreshToken;
import com.example.delservice.model.User;
import org.springframework.stereotype.Service;

import java.util.Optional;

public interface RefreshTokenService {

    Optional<RefreshToken> findByToken(String token);

    RefreshToken createRefreshToken(String username);

    RefreshToken verifyExpiration(RefreshToken token);

    void deleteByUser(Long userId);
}
