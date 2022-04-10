package com.example.delservice.service.impl;

import com.example.delservice.config.jwt.Exception.TokenRefreshException;
import com.example.delservice.model.RefreshToken;
import com.example.delservice.model.User;
import com.example.delservice.repository.RefreshTokenRepository;
import com.example.delservice.repository.UserRepository;
import com.example.delservice.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    @Value("${jwt.refresh_token_duration}")
    private Long refreshTokenDurationMs;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;


    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent() && refreshTokenRepository.findByUser(user.get()).isPresent()) {
            refreshTokenRepository.deleteByUser(user.get());
        }
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user.get());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken = refreshTokenRepository.save(refreshToken);
        return refreshToken;
    }

    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0){
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(),
                    "Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    @Transactional
    @Override
    public void deleteByUser(Long userId) {
        refreshTokenRepository.deleteByUser(userRepository.findById(userId).get());
    }
}
