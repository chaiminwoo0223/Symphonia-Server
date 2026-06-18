package com.symphonia.auth.domain.repository;

import com.symphonia.auth.infrastructure.redis.BlacklistAccessToken;

public interface BlacklistAccessTokenRepository {
    void save(BlacklistAccessToken blacklistAccessToken);

    boolean isBlacklisted(String accessToken);
}
