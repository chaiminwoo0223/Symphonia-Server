package com.symphonia.auth.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.RedisRepositoryTest;
import com.symphonia.auth.domain.repository.RefreshTokenRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(RefreshTokenRepositoryImpl.class)
class RefreshTokenRepositoryImplTest extends RedisRepositoryTest {

    private static final String VALUE = "refresh-token-value";
    private static final String MEMBER_ID = "1";
    private static final Long EXPIRATION_TIME = 3600L;

    @Autowired private RefreshTokenRepository refreshTokenRepository;

    @Nested
    @DisplayName("save 메서드는")
    class Save {

        @Test
        @DisplayName("리프레시 토큰을 저장한다")
        void shouldPersistRefreshToken() {
            // when
            refreshTokenRepository.save(VALUE, MEMBER_ID, EXPIRATION_TIME);

            // then
            assertThat(refreshTokenRepository.findMemberIdByValue(VALUE)).contains(MEMBER_ID);
        }
    }

    @Nested
    @DisplayName("findMemberIdByValue 메서드는")
    class FindMemberIdByValue {

        @Test
        @DisplayName("존재하지 않는 값이면 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenValueNotExists() {
            // when
            Optional<String> result = refreshTokenRepository.findMemberIdByValue("unknown");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("delete 메서드는")
    class Delete {

        @Test
        @DisplayName("memberId로 리프레시 토큰을 삭제한다")
        void shouldRemoveRefreshToken() {
            // given
            refreshTokenRepository.save(VALUE, MEMBER_ID, EXPIRATION_TIME);

            // when
            refreshTokenRepository.delete(MEMBER_ID);

            // then
            assertThat(refreshTokenRepository.findMemberIdByValue(VALUE)).isEmpty();
        }
    }
}
