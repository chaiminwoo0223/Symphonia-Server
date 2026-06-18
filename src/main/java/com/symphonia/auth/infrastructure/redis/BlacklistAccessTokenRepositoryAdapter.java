package com.symphonia.auth.infrastructure.redis;

import com.symphonia.auth.domain.repository.BlacklistAccessTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BlacklistAccessTokenRepositoryAdapter implements BlacklistAccessTokenRepository {
    private final BlacklistAccessTokenRedisRepository blacklistAccessTokenRedisRepository;

    @Override
    public void save(BlacklistAccessToken blacklistAccessToken) {
        blacklistAccessTokenRedisRepository.save(blacklistAccessToken);
    }

    @Override
    public boolean isBlacklisted(String accessToken) {
        return blacklistAccessTokenRedisRepository.existsById(accessToken);
    }
}
