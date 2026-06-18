package com.symphonia.auth.domain.repository;

public interface BlacklistAccessTokenRepository {
    void save(String accessToken, String memberId, Long expirationTime);

    boolean isBlacklisted(String accessToken);
}
