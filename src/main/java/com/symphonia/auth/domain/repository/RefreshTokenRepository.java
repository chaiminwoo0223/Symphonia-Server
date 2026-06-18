package com.symphonia.auth.domain.repository;

public interface RefreshTokenRepository {
    void save(String memberId, String refreshToken);

    String find(String memberId);

    void delete(String memberId);
}
