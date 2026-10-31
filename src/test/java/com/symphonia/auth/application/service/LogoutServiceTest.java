package com.symphonia.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.symphonia.UnitTest;
import com.symphonia.auth.application.dto.command.LogoutCommand;
import com.symphonia.auth.application.dto.result.LogoutResult;
import com.symphonia.auth.domain.provider.AccessTokenProvider;
import com.symphonia.auth.domain.repository.BlacklistAccessTokenRepository;
import com.symphonia.auth.domain.repository.RefreshTokenRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("LogoutService 단위 테스트")
class LogoutServiceTest extends UnitTest {

    private static final String MEMBER_ID = "1";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String IP = "127.0.0.1";
    private static final long ACCESS_TOKEN_REMAINING_TIME = 3600L;

    @InjectMocks private LogoutService logoutService;

    @Mock private AccessTokenProvider accessTokenProvider;

    @Mock private RefreshTokenRepository refreshTokenRepository;

    @Mock private BlacklistAccessTokenRepository blacklistAccessTokenRepository;

    @Nested
    @DisplayName("logout 메서드는")
    class Logout {

        @Nested
        @DisplayName("리프레시 토큰과 유효한 엑세스 토큰이 있는 경우")
        class WhenTokensAreValid {

            @Test
            @DisplayName("이 기기의 리프레시 토큰만 지우고 엑세스 토큰을 블랙리스트에 등록한다.")
            void shouldConsumeRefreshTokenAndBlacklistAccessToken() {
                // given
                given(refreshTokenRepository.consume(REFRESH_TOKEN))
                        .willReturn(Optional.of(MEMBER_ID));
                given(accessTokenProvider.validate(ACCESS_TOKEN)).willReturn(true);
                given(accessTokenProvider.getMemberId(ACCESS_TOKEN)).willReturn(MEMBER_ID);
                given(accessTokenProvider.getRemainingTime(ACCESS_TOKEN))
                        .willReturn(ACCESS_TOKEN_REMAINING_TIME);

                // when
                LogoutResult result =
                        logoutService.logout(LogoutCommand.of(REFRESH_TOKEN, ACCESS_TOKEN, IP));

                // then
                assertThat(result.memberId()).isEqualTo(MEMBER_ID);
                verify(refreshTokenRepository, never()).delete(any());
                verify(blacklistAccessTokenRepository)
                        .save(ACCESS_TOKEN, MEMBER_ID, ACCESS_TOKEN_REMAINING_TIME);
            }
        }

        @Nested
        @DisplayName("엑세스 토큰이 만료된 경우")
        class WhenAccessTokenExpired {

            @Test
            @DisplayName("리프레시 토큰만 지우고 엑세스 토큰은 블랙리스트에 등록하지 않는다.")
            void shouldConsumeRefreshTokenWithoutBlacklisting() {
                // given
                given(refreshTokenRepository.consume(REFRESH_TOKEN))
                        .willReturn(Optional.of(MEMBER_ID));
                given(accessTokenProvider.validate(ACCESS_TOKEN)).willReturn(false);

                // when
                LogoutResult result =
                        logoutService.logout(LogoutCommand.of(REFRESH_TOKEN, ACCESS_TOKEN, IP));

                // then
                assertThat(result.memberId()).isEqualTo(MEMBER_ID);
                verifyNoInteractions(blacklistAccessTokenRepository);
            }
        }

        @Nested
        @DisplayName("토큰이 모두 없는 경우")
        class WhenTokensAreMissing {

            @Test
            @DisplayName("예외 없이 회원 ID가 없는 결과를 반환한다.")
            void shouldReturnResultWithoutMemberId() {
                // when
                LogoutResult result = logoutService.logout(LogoutCommand.of(null, null, IP));

                // then
                assertThat(result.memberId()).isNull();
                verifyNoInteractions(refreshTokenRepository, blacklistAccessTokenRepository);
            }
        }
    }
}
