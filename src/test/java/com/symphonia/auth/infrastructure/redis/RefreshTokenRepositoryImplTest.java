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
    @Autowired private RefreshTokenRedisRepository refreshTokenRedisRepository;

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

        @Test
        @DisplayName("원본 토큰 값이 아니라 해시된 값을 키로 저장한다")
        void shouldPersistHashedValueInsteadOfRawValue() {
            // when
            refreshTokenRepository.save(VALUE, MEMBER_ID, EXPIRATION_TIME);

            // then
            assertThat(refreshTokenRedisRepository.existsById(VALUE)).isFalse();
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
    @DisplayName("consume 메서드는")
    class Consume {

        @Nested
        @DisplayName("존재하는 토큰인 경우")
        class WhenTokenExists {

            // consume()이 거는 락은 짧은 TTL 동안 남아있으므로, 다른 Consume 테스트와 값을 공유하지 않는다.
            private static final String CONSUMABLE_VALUE = "consumable-refresh-token-value";
            private static final String REMOVED_VALUE = "removed-refresh-token-value";
            private static final String DEVICE_A_VALUE = "device-a-refresh-token-value";
            private static final String DEVICE_B_VALUE = "device-b-refresh-token-value";

            // 다른 테스트가 실제 회원 ID로 남긴 토큰과 섞이지 않도록 숫자가 아닌 ID를 쓴다.
            private static final String DEVICE_OWNER_ID = "device-owner-member";

            @Test
            @DisplayName("멤버 ID를 반환한다")
            void shouldReturnMemberId() {
                // given
                refreshTokenRepository.save(CONSUMABLE_VALUE, MEMBER_ID, EXPIRATION_TIME);

                // when
                Optional<String> result = refreshTokenRepository.consume(CONSUMABLE_VALUE);

                // then
                assertThat(result).contains(MEMBER_ID);
            }

            @Test
            @DisplayName("소비한 토큰을 삭제한다")
            void shouldRemoveConsumedToken() {
                // given
                refreshTokenRepository.save(REMOVED_VALUE, MEMBER_ID, EXPIRATION_TIME);

                // when
                refreshTokenRepository.consume(REMOVED_VALUE);

                // then
                assertThat(refreshTokenRepository.findMemberIdByValue(REMOVED_VALUE)).isEmpty();
            }

            @Test
            @DisplayName("같은 회원의 다른 토큰은 삭제하지 않는다")
            void shouldKeepOtherTokensOfSameMember() {
                // given
                refreshTokenRepository.save(DEVICE_A_VALUE, DEVICE_OWNER_ID, EXPIRATION_TIME);
                refreshTokenRepository.save(DEVICE_B_VALUE, DEVICE_OWNER_ID, EXPIRATION_TIME);

                // when
                refreshTokenRepository.consume(DEVICE_A_VALUE);

                // then
                assertThat(refreshTokenRepository.findMemberIdByValue(DEVICE_B_VALUE))
                        .contains(DEVICE_OWNER_ID);
                assertThat(refreshTokenRedisRepository.findAllByMemberId(DEVICE_OWNER_ID))
                        .hasSize(1);
            }
        }

        @Nested
        @DisplayName("존재하지 않는 토큰인 경우")
        class WhenTokenNotExists {

            @Test
            @DisplayName("빈 Optional을 반환한다")
            void shouldReturnEmpty() {
                // when
                Optional<String> result = refreshTokenRepository.consume("unknown");

                // then
                assertThat(result).isEmpty();
            }
        }

        @Nested
        @DisplayName("같은 토큰이 이미 소비 중인 경우")
        class WhenTokenAlreadyBeingConsumed {

            private static final String ANOTHER_VALUE = "another-refresh-token-value";

            @Test
            @DisplayName("빈 Optional을 반환한다")
            void shouldReturnEmpty() {
                // given
                refreshTokenRepository.save(ANOTHER_VALUE, MEMBER_ID, EXPIRATION_TIME);
                refreshTokenRepository.consume(ANOTHER_VALUE);

                // when
                Optional<String> result = refreshTokenRepository.consume(ANOTHER_VALUE);

                // then
                assertThat(result).isEmpty();
            }
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
