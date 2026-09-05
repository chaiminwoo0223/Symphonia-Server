package com.symphonia.auth.domain.repository;

import java.util.Optional;

public interface RefreshTokenRepository {
    void save(String value, String memberId, Long expirationTime);

    Optional<String> findMemberIdByValue(String value);

    void delete(String memberId);
}
