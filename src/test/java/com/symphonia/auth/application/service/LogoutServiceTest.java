package com.symphonia.auth.application.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.symphonia.UnitTest;
import com.symphonia.auth.domain.repository.BlacklistAccessTokenRepository;
import com.symphonia.auth.domain.repository.RefreshTokenRepository;
import com.symphonia.auth.infrastructure.provider.AccessTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("LogoutService 단위 테스트")
class LogoutServiceTest extends UnitTest {

    @InjectMocks private LogoutService logoutService;

    @Mock private AccessTokenProvider accessTokenProvider;

    @Mock private RefreshTokenRepository refreshTokenRepository;

    @Mock private BlacklistAccessTokenRepository blacklistAccessTokenRepository;

    private static final String MEMBER_ID = "1";
    private static final String ACCESS_TOKEN = "access-token";
    private static final long ACCESS_TOKEN_REMAINING_TIME = 3600L;

    @Nested
    @DisplayName("Logout")
    class Logout {

        @BeforeEach
        void setUp() {
            given(accessTokenProvider.getMemberId(ACCESS_TOKEN)).willReturn(MEMBER_ID);
            given(accessTokenProvider.getRemainingTime(ACCESS_TOKEN))
                    .willReturn(ACCESS_TOKEN_REMAINING_TIME);
        }

        @Test
        @DisplayName("엑세스 토큰을 블랙리스트에 등록한다.")
        void shouldRegisterAccessTokenToBlacklistWhenLoggedOut() {
            // when
            logoutService.logout(ACCESS_TOKEN);

            // then
            verify(blacklistAccessTokenRepository)
                    .save(ACCESS_TOKEN, MEMBER_ID, ACCESS_TOKEN_REMAINING_TIME);
        }

        @Test
        @DisplayName("해당 멤버의 리프레시 토큰을 삭제한다.")
        void shouldDeleteRefreshTokenWhenLoggedOut() {
            // when
            logoutService.logout(ACCESS_TOKEN);

            // then
            verify(refreshTokenRepository).delete(MEMBER_ID);
        }
    }
}
