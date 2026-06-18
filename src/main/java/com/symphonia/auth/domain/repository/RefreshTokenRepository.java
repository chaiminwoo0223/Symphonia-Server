package com.symphonia.auth.domain.repository;

import com.symphonia.auth.infrastructure.redis.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository {
    void save(RefreshToken refreshToken);

    Optional<String> find(String memberId);

    void delete(String memberId);
}
