package com.symphonia.auth.infrastructure.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.symphonia.UnitTest;
import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.auth.domain.exception.InvalidJwtTokenException;
import com.symphonia.global.config.properties.AccessTokenProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AccessTokenProvider 단위 테스트")
class AccessTokenProviderTest extends UnitTest {

    private static final String SECRET =
            "test-only-fixed-jwt-secret-for-symphonia-integration-tests-do-not-use-in-prod";
    private static final long EXPIRATION_TIME = 7200L;
    private static final String MEMBER_ID = "1";
    private static final String ROLE = "ROLE_MEMBER";
    private static final String MALFORMED_TOKEN = "invalid.jwt.token";

    private AccessTokenProvider accessTokenProvider;

    @BeforeEach
    void setUp() {
        accessTokenProvider =
                new AccessTokenProvider(new AccessTokenProperties(SECRET, EXPIRATION_TIME));
    }

    @Nested
    @DisplayName("generate 메서드는")
    class Generate {

        @Test
        @DisplayName("멤버 ID와 권한을 담은 토큰을 발급한다.")
        void shouldEmbedMemberIdAndRoleInToken() {
            // when
            String accessToken = accessTokenProvider.generate(MEMBER_ID, ROLE);

            // then
            assertThat(accessTokenProvider.getMemberId(accessToken)).isEqualTo(MEMBER_ID);
            assertThat(accessTokenProvider.getRole(accessToken)).isEqualTo(ROLE);
        }
    }

    @Nested
    @DisplayName("getMemberId 메서드는")
    class GetMemberId {

        @Test
        @DisplayName("파싱할 수 없는 토큰이면 InvalidJwtTokenException을 던진다.")
        void shouldThrowInvalidJwtTokenExceptionWhenTokenIsMalformed() {
            // when & then
            assertThatThrownBy(() -> accessTokenProvider.getMemberId(MALFORMED_TOKEN))
                    .isInstanceOf(InvalidJwtTokenException.class)
                    .hasMessage(AuthErrorCode.INVALID_JWT_TOKEN.getMessage());
        }
    }

    @Nested
    @DisplayName("validate 메서드는")
    class Validate {

        @Nested
        @DisplayName("만료된 토큰인 경우")
        class WhenTokenIsExpired {

            @Test
            @DisplayName("false를 반환한다.")
            void shouldReturnFalse() {
                // given
                AccessTokenProvider expiredTokenProvider =
                        new AccessTokenProvider(new AccessTokenProperties(SECRET, -1L));
                String expiredToken = expiredTokenProvider.generate(MEMBER_ID, ROLE);

                // when
                boolean result = accessTokenProvider.validate(expiredToken);

                // then
                assertThat(result).isFalse();
            }
        }

        @Nested
        @DisplayName("위조된 토큰인 경우")
        class WhenTokenIsTampered {

            @Test
            @DisplayName("false를 반환한다.")
            void shouldReturnFalse() {
                // given
                String validToken = accessTokenProvider.generate(MEMBER_ID, ROLE);
                int tamperIndex = validToken.length() / 2;
                char originalChar = validToken.charAt(tamperIndex);
                char tamperedChar = originalChar == 'a' ? 'b' : 'a';
                String tamperedToken =
                        validToken.substring(0, tamperIndex)
                                + tamperedChar
                                + validToken.substring(tamperIndex + 1);

                // when
                boolean result = accessTokenProvider.validate(tamperedToken);

                // then
                assertThat(result).isFalse();
            }
        }
    }
}
