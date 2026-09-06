package com.symphonia.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.symphonia.UnitTest;
import com.symphonia.auth.application.dto.result.TokenResult;
import com.symphonia.auth.domain.repository.RefreshTokenRepository;
import com.symphonia.auth.infrastructure.provider.AccessTokenProvider;
import com.symphonia.auth.infrastructure.provider.RefreshTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("IssueTokenService 단위 테스트")
class IssueTokenServiceTest extends UnitTest {

    @InjectMocks private IssueTokenService issueTokenService;

    @Mock private AccessTokenProvider accessTokenProvider;

    @Mock private RefreshTokenProvider refreshTokenProvider;

    @Mock private RefreshTokenRepository refreshTokenRepository;

    private static final String MEMBER_ID = "1";
    private static final String ROLE = "ROLE_USER";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final long REFRESH_TOKEN_EXPIRATION_TIME = 1209600L;

    @Nested
    @DisplayName("Issue")
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
            issueTokenService.issue(MEMBER_ID, ROLE);

            // then
            verify(accessTokenProvider).generate(MEMBER_ID, ROLE);
            verify(refreshTokenProvider).generate();
        }

        @Test
        @DisplayName("생성한 리프레시 토큰을 저장한다.")
        void shouldSaveRefreshTokenWhenIssued() {
            // when
            issueTokenService.issue(MEMBER_ID, ROLE);

            // then
            verify(refreshTokenRepository)
                    .save(REFRESH_TOKEN, MEMBER_ID, REFRESH_TOKEN_EXPIRATION_TIME);
        }

        @Test
        @DisplayName("TokenResult를 반환한다.")
        void shouldReturnTokenResultWhenIssued() {
            // when
            TokenResult result = issueTokenService.issue(MEMBER_ID, ROLE);

            // then
            assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
        }
    }
}
