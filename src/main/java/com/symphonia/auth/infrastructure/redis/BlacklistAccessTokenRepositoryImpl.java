package com.symphonia.auth.infrastructure.redis;

import com.symphonia.auth.domain.repository.BlacklistAccessTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BlacklistAccessTokenRepositoryImpl implements BlacklistAccessTokenRepository {
    private final BlacklistAccessTokenRedisRepository blacklistAccessTokenRedisRepository;

    @Override
    public void save(String accessToken, String memberId, Long expirationTime) {
        BlacklistAccessToken blacklistAccessToken =
                BlacklistAccessToken.of(accessToken, memberId, expirationTime);

        blacklistAccessTokenRedisRepository.save(blacklistAccessToken);
    }

    @Override
    public boolean isBlacklisted(String accessToken) {
        return blacklistAccessTokenRedisRepository.existsById(accessToken);
    }
}
