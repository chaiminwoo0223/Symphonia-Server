package com.symphonia.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.auth.application.dto.result.OAuthMemberResult;
import com.symphonia.auth.domain.client.SocialClient;
import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.auth.domain.exception.UnsupportedSocialProviderException;
import com.symphonia.auth.domain.identity.SocialIdentity;
import com.symphonia.auth.fixture.SocialIdentityFixture;
import com.symphonia.member.domain.entity.SocialProvider;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@DisplayName("ExchangeSocialCodeService 단위 테스트")
class ExchangeSocialCodeServiceTest extends UnitTest {

    private static final String CODE = "auth-code";
    private static final String KAKAO = "kakao";
    private static final String GOOGLE = "google";

    private ExchangeSocialCodeService exchangeSocialCodeService;

    @Mock private SocialClient kakaoSocialClient;

    @Mock private SocialClient googleSocialClient;

    @BeforeEach
    void setUp() {
        exchangeSocialCodeService =
                new ExchangeSocialCodeService(
                        Map.of(KAKAO, kakaoSocialClient, GOOGLE, googleSocialClient));
    }

    @Nested
    @DisplayName("exchange 메서드는")
    class Exchange {

        @Nested
        @DisplayName("provider가 kakao인 경우")
        class WhenProviderIsKakao {

            @Test
            @DisplayName("kakao SocialClient로 인증하고 KAKAO OAuthMemberResult를 반환한다.")
            void shouldReturnOAuthMemberResultFromKakaoClient() {
                // given
                SocialIdentity identity = SocialIdentityFixture.KAKAO.create();
                given(kakaoSocialClient.authenticate(CODE)).willReturn(identity);

                // when
                OAuthMemberResult result = exchangeSocialCodeService.exchange(KAKAO, CODE);

                // then
                assertThat(result.socialId()).isEqualTo(identity.socialId());
                assertThat(result.nickname()).isEqualTo(identity.nickname());
                assertThat(result.email()).isEqualTo(identity.email());
                assertThat(result.profileImage()).isEqualTo(identity.profileImage());
                assertThat(result.socialProvider()).isEqualTo(SocialProvider.KAKAO);
                then(googleSocialClient).shouldHaveNoInteractions();
            }

            @Test
            @DisplayName("provider 대소문자가 섞여 있어도 kakao SocialClient로 인증한다.")
            void shouldReturnOAuthMemberResultWhenProviderCasingIsMixed() {
                // given
                SocialIdentity identity = SocialIdentityFixture.KAKAO.create();
                given(kakaoSocialClient.authenticate(CODE)).willReturn(identity);

                // when
                OAuthMemberResult result = exchangeSocialCodeService.exchange("KaKao", CODE);

                // then
                assertThat(result.socialProvider()).isEqualTo(SocialProvider.KAKAO);
            }
        }

        @Nested
        @DisplayName("provider가 google인 경우")
        class WhenProviderIsGoogle {

            @Test
            @DisplayName("google SocialClient로 인증하고 GOOGLE OAuthMemberResult를 반환한다.")
            void shouldReturnOAuthMemberResultFromGoogleClient() {
                // given
                SocialIdentity identity = SocialIdentityFixture.GOOGLE.create();
                given(googleSocialClient.authenticate(CODE)).willReturn(identity);

                // when
                OAuthMemberResult result = exchangeSocialCodeService.exchange(GOOGLE, CODE);

                // then
                assertThat(result.socialId()).isEqualTo(identity.socialId());
                assertThat(result.nickname()).isEqualTo(identity.nickname());
                assertThat(result.email()).isEqualTo(identity.email());
                assertThat(result.profileImage()).isEqualTo(identity.profileImage());
                assertThat(result.socialProvider()).isEqualTo(SocialProvider.GOOGLE);
                then(kakaoSocialClient).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("등록되지 않은 provider인 경우")
        class WhenProviderIsUnsupported {

            @Test
            @DisplayName("예외가 발생한다.")
            void shouldThrowException() {
                // when & then
                assertThatThrownBy(() -> exchangeSocialCodeService.exchange("naver", CODE))
                        .isInstanceOf(UnsupportedSocialProviderException.class)
                        .hasMessage(AuthErrorCode.UNSUPPORTED_SOCIAL_PROVIDER.getMessage());
                then(kakaoSocialClient).shouldHaveNoInteractions();
                then(googleSocialClient).shouldHaveNoInteractions();
            }
        }
    }
}
