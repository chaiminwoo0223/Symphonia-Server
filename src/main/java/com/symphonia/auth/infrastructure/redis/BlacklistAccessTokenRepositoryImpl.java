package com.symphonia.auth.infrastructure.redis;

import static com.symphonia.auth.infrastructure.redis.RedisFailureTranslator.call;
import static com.symphonia.auth.infrastructure.redis.RedisFailureTranslator.run;

import com.symphonia.auth.domain.repository.BlacklistAccessTokenRepository;
import com.symphonia.common.crypto.Sha256;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BlacklistAccessTokenRepositoryImpl implements BlacklistAccessTokenRepository {
    // @TimeToLive가 0이면 Redis 키에 만료가 걸리지 않고 영구히 남는다.
    private static final long MIN_TTL_SECONDS = 1L;

    private final BlacklistAccessTokenRedisRepository blacklistAccessTokenRedisRepository;

    @Override
    public void save(String accessToken, String memberId, Long expirationTime) {
        BlacklistAccessToken blacklistAccessToken =
                BlacklistAccessToken.of(
                        Sha256.hash(accessToken),
                        memberId,
                        Math.max(expirationTime, MIN_TTL_SECONDS));

        run(() -> blacklistAccessTokenRedisRepository.save(blacklistAccessToken));
    }

    @Override
    public boolean isBlacklisted(String accessToken) {
        return call(() -> blacklistAccessTokenRedisRepository.existsById(Sha256.hash(accessToken)));
    }
}
