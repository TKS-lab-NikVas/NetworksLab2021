package com.example.delservice.repository;

import com.example.delservice.model.RefreshToken;
import com.example.delservice.model.User;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, Long> {

    Optional<RefreshToken> findById(Long id);

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);


}
