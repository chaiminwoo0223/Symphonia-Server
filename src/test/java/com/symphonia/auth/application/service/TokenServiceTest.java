package com.symphonia.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.symphonia.UnitTest;
import com.symphonia.auth.application.dto.result.TokenResult;
import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.auth.domain.repository.BlacklistAccessTokenRepository;
import com.symphonia.auth.domain.repository.RefreshTokenRepository;
import com.symphonia.auth.infrastructure.provider.AccessTokenProvider;
import com.symphonia.auth.infrastructure.provider.RefreshTokenProvider;
import com.symphonia.global.exception.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("TokenService 단위 테스트")
class TokenServiceTest extends UnitTest {

    @InjectMocks private TokenService tokenService;

    @Mock private AccessTokenProvider accessTokenProvider;

    @Mock private RefreshTokenProvider refreshTokenProvider;

    @Mock private RefreshTokenRepository refreshTokenRepository;

    @Mock private BlacklistAccessTokenRepository blacklistAccessTokenRepository;

    private static final String MEMBER_ID = "1";
    private static final String ROLE = "ROLE_USER";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String NEW_ACCESS_TOKEN = "new-access-token";
    private static final String NEW_REFRESH_TOKEN = "new-refresh-token";
    private static final long ACCESS_TOKEN_REMAINING_TIME = 3600L;
    private static final long REFRESH_TOKEN_EXPIRATION_TIME = 1209600L;

    @Nested
    @DisplayName("issue 메서드는")
    class Issue {

        @BeforeEach
        void setUp() {
            given(accessTokenProvider.generate(MEMBER_ID, ROLE)).willReturn(ACCESS_TOKEN);
            given(refreshTokenProvider.generate()).willReturn(REFRESH_TOKEN);
            given(refreshTokenProvider.getExpirationTime())
                    .willReturn(REFRESH_TOKEN_EXPIRATION_TIME);
        }

        @Test
        @DisplayName("엑세스 토큰과 리프레시 토큰을 생성한다.")
        void shouldGenerateAccessTokenAndRefreshTokenWhenIssued() {
            // when
            tokenService.issue(MEMBER_ID, ROLE);

            // then
            verify(accessTokenProvider).generate(MEMBER_ID, ROLE);
            verify(refreshTokenProvider).generate();
        }

        @Test
        @DisplayName("생성한 리프레시 토큰을 저장한다.")
        void shouldSaveRefreshTokenWhenIssued() {
            // when
            tokenService.issue(MEMBER_ID, ROLE);

            // then
            verify(refreshTokenRepository)
                    .save(REFRESH_TOKEN, MEMBER_ID, REFRESH_TOKEN_EXPIRATION_TIME);
        }

        @Test
        @DisplayName("TokenResult를 반환한다.")
        void shouldReturnTokenResultWhenIssued() {
            // when
            TokenResult result = tokenService.issue(MEMBER_ID, ROLE);

            // then
            assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
        }
    }

    @Nested
    @DisplayName("reissue 메서드는")
    class Reissue {

        @BeforeEach
        void setUp() {
            given(accessTokenProvider.generate(MEMBER_ID, ROLE)).willReturn(NEW_ACCESS_TOKEN);
            given(refreshTokenProvider.generate()).willReturn(NEW_REFRESH_TOKEN);
            given(refreshTokenProvider.getExpirationTime())
                    .willReturn(REFRESH_TOKEN_EXPIRATION_TIME);
        }

        @Test
        @DisplayName("기존 리프레시 토큰을 삭제한다.")
        void shouldDeleteExistingRefreshTokenWhenReissued() {
            // when
            tokenService.reissue(MEMBER_ID, ROLE);

            // then
            verify(refreshTokenRepository).delete(MEMBER_ID);
        }

        @Test
        @DisplayName("새로운 엑세스 토큰과 새로운 리프레시 토큰을 생성한다.")
        void shouldGenerateNewAccessTokenAndRefreshTokenWhenReissued() {
            // when
            tokenService.reissue(MEMBER_ID, ROLE);

            // then
            verify(accessTokenProvider).generate(MEMBER_ID, ROLE);
            verify(refreshTokenProvider).generate();
        }

        @Test
        @DisplayName("새로운 리프레시 토큰을 저장한다.")
        void shouldSaveNewRefreshTokenWhenReissued() {
            // when
            tokenService.reissue(MEMBER_ID, ROLE);

            // then
            verify(refreshTokenRepository)
                    .save(NEW_REFRESH_TOKEN, MEMBER_ID, REFRESH_TOKEN_EXPIRATION_TIME);
        }

        @Test
        @DisplayName("새로운 TokenResult를 반환한다.")
        void shouldReturnNewTokenResultWhenReissued() {
            // when
            TokenResult result = tokenService.reissue(MEMBER_ID, ROLE);

            // then
            assertThat(result.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
            assertThat(result.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
        }
    }

    @Nested
    @DisplayName("revoke 메서드는")
    class Revoke {

        @BeforeEach
        void setUp() {
            given(accessTokenProvider.getMemberId(ACCESS_TOKEN)).willReturn(MEMBER_ID);
            given(accessTokenProvider.getRemainingTime(ACCESS_TOKEN))
                    .willReturn(ACCESS_TOKEN_REMAINING_TIME);
        }

        @Test
        @DisplayName("엑세스 토큰을 블랙리스트에 등록한다.")
        void shouldRegisterAccessTokenToBlacklistWhenRevoked() {
            // when
            tokenService.revoke(ACCESS_TOKEN);

            // then
            verify(blacklistAccessTokenRepository)
                    .save(ACCESS_TOKEN, MEMBER_ID, ACCESS_TOKEN_REMAINING_TIME);
        }

        @Test
        @DisplayName("해당 멤버의 리프레시 토큰을 삭제한다.")
        void shouldDeleteRefreshTokenWhenRevoked() {
            // when
            tokenService.revoke(ACCESS_TOKEN);

            // then
            verify(refreshTokenRepository).delete(MEMBER_ID);
        }
    }

    @Nested
    @DisplayName("forceRevoke 메서드는")
    class ForceRevoke {

        @Test
        @DisplayName("관리자가 강제로 무효화하면 해당 멤버의 리프레시 토큰을 삭제한다.")
        void shouldDeleteRefreshTokenWhenForceRevokedByAdmin() {
            // when
            tokenService.forceRevoke(MEMBER_ID);

            // then
            verify(refreshTokenRepository).delete(MEMBER_ID);
        }
    }

    @Nested
    @DisplayName("getMemberId 메서드는")
    class GetMemberId {

        @Test
        @DisplayName("리프레시 토큰에 해당하는 memberId를 반환한다.")
        void shouldReturnMemberIdWhenRefreshTokenFound() {
            // given
            given(refreshTokenRepository.findMemberIdByValue(REFRESH_TOKEN))
                    .willReturn(Optional.of(MEMBER_ID));

            // when
            String result = tokenService.getMemberId(REFRESH_TOKEN);

            // then
            assertThat(result).isEqualTo(MEMBER_ID);
        }

        @Test
        @DisplayName("리프레시 토큰에 해당하는 멤버가 없으면 예외가 발생한다.")
        void shouldThrowExceptionWhenRefreshTokenNotFound() {
            // given
            given(refreshTokenRepository.findMemberIdByValue(REFRESH_TOKEN))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> tokenService.getMemberId(REFRESH_TOKEN))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND.getMessage());
        }
    }
}
