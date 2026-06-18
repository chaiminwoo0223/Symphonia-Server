package com.symphonia.auth.domain.repository;

import java.util.Optional;

public interface RefreshTokenRepository {
    void save(String memberId, String value, Long expirationTime);

    Optional<String> find(String memberId);

    void delete(String memberId);
}
