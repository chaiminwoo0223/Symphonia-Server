package com.symphonia.auth.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RedisRepositoryTest;
import com.symphonia.auth.domain.repository.BlacklistAccessTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(BlacklistAccessTokenRepositoryImpl.class)
class BlacklistAccessTokenRepositoryImplTest extends RedisRepositoryTest {

    private static final String ACCESS_TOKEN = "access-token-value";
    private static final String MEMBER_ID = "1";
    private static final Long EXPIRATION_TIME = 3600L;

    @Autowired private BlacklistAccessTokenRepository blacklistAccessTokenRepository;

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
