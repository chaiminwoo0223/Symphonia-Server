package com.symphonia.auth.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RedisRepositoryTest;
import com.symphonia.auth.domain.repository.BlacklistAccessTokenRepository;
import com.symphonia.common.crypto.Sha256;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

@Import(BlacklistAccessTokenRepositoryImpl.class)
class BlacklistAccessTokenRepositoryImplTest extends RedisRepositoryTest {

    private static final String ACCESS_TOKEN = "access-token-value";
    private static final String MEMBER_ID = "1";
    private static final Long EXPIRATION_TIME = 3600L;

    @Autowired private BlacklistAccessTokenRepository blacklistAccessTokenRepository;
    @Autowired private BlacklistAccessTokenRedisRepository blacklistAccessTokenRedisRepository;
    @Autowired private StringRedisTemplate redisTemplate;

    @Nested
    @DisplayName("save 메서드는")
    class Save {

        @Test
        @DisplayName("액세스 토큰을 블랙리스트에 등록한다")
        void shouldRegisterAccessTokenAsBlacklisted() {
            // when
            blacklistAccessTokenRepository.save(ACCESS_TOKEN, MEMBER_ID, EXPIRATION_TIME);

            // then
            assertThat(blacklistAccessTokenRepository.isBlacklisted(ACCESS_TOKEN)).isTrue();
        }

        @Test
        @DisplayName("원본 토큰 값이 아니라 해시된 값을 키로 저장한다")
        void shouldPersistHashedValueInsteadOfRawValue() {
            // when
            blacklistAccessTokenRepository.save(ACCESS_TOKEN, MEMBER_ID, EXPIRATION_TIME);

            // then
            assertThat(blacklistAccessTokenRedisRepository.existsById(ACCESS_TOKEN)).isFalse();
        }

        @Test
        @DisplayName("남은 시간이 0초여도 키가 만료되도록 최소 1초를 적용한다")
        void shouldApplyMinimumTtlWhenRemainingTimeIsZero() {
            // when
            blacklistAccessTokenRepository.save(ACCESS_TOKEN, MEMBER_ID, 0L);

            // then
            Long ttl =
                    redisTemplate.getExpire("blacklist_access_token:" + Sha256.hash(ACCESS_TOKEN));
            assertThat(ttl).isPositive();
        }
    }

    @Nested
    @DisplayName("isBlacklisted 메서드는")
    class IsBlacklisted {

        @Test
        @DisplayName("등록되지 않은 토큰이면 false를 반환한다")
        void shouldReturnFalseWhenAccessTokenNotBlacklisted() {
            // when
            boolean result = blacklistAccessTokenRepository.isBlacklisted("unknown");

            // then
            assertThat(result).isFalse();
        }
    }
}
