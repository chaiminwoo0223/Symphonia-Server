package com.symphonia.auth.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphonia.IntegrationTest;
import com.symphonia.auth.domain.client.SocialClient;
import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.auth.domain.exception.InvalidSocialTokenException;
import com.symphonia.auth.domain.exception.SocialRequiredInfoMissingException;
import com.symphonia.auth.domain.repository.BlacklistAccessTokenRepository;
import com.symphonia.auth.domain.repository.RefreshTokenRepository;
import com.symphonia.auth.fixture.SocialIdentityFixture;
import com.symphonia.auth.helper.AuthHelper;
import com.symphonia.auth.infrastructure.provider.JwtAccessTokenProvider;
import com.symphonia.auth.presentation.dto.request.LoginRequest;
import com.symphonia.auth.presentation.dto.request.LogoutRequest;
import com.symphonia.auth.presentation.dto.request.RefreshRequest;
import com.symphonia.auth.presentation.dto.request.SignupRequest;
import com.symphonia.global.config.properties.AccessTokenProperties;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.entity.SocialProvider;
import com.symphonia.member.domain.repository.MemberRepository;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class AuthControllerTest extends IntegrationTest {

    private static final String SOCIAL_TOKEN = "social-token";

    @Autowired private MemberHelper memberHelper;
    @Autowired private AuthHelper authHelper;
    @Autowired private BlacklistAccessTokenRepository blacklistAccessTokenRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private AccessTokenProperties accessTokenProperties;

    @MockitoBean(name = "kakao")
    private SocialClient kakaoSocialClient;

    @MockitoBean(name = "google")
    private SocialClient googleSocialClient;

    @Nested
    @DisplayName("POST /api/v1/auth/signup은")
    class Signup {

        @Nested
        @DisplayName("지원하는 provider의 유효한 소셜 토큰인 경우")
        class WhenSocialTokenIsValid {

            @Test
            @DisplayName("멤버를 생성하고 201과 함께 토큰을 반환한다")
            void shouldCreateMemberAndReturnToken() throws Exception {
                // given
                given(kakaoSocialClient.authenticate(SOCIAL_TOKEN))
                        .willReturn(SocialIdentityFixture.KAKAO.create());
                SignupRequest request = new SignupRequest("kakao", SOCIAL_TOKEN);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/signup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.accessToken").exists())
                        .andExpect(jsonPath("$.data.refreshToken").exists())
                        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
                assertThat(
                                memberRepository.existsBySocialLogin(
                                        SocialProvider.KAKAO,
                                        SocialIdentityFixture.KAKAO.getSocialId()))
                        .isTrue();
            }
        }

        @Nested
        @DisplayName("이미 가입된 소셜 계정인 경우")
        class WhenSocialAccountAlreadyRegistered {

            @Test
            @DisplayName("409를 반환한다")
            void shouldReturnConflict() throws Exception {
                // given
                memberHelper.save(MemberFixture.KAKAO);
                given(kakaoSocialClient.authenticate(SOCIAL_TOKEN))
                        .willReturn(SocialIdentityFixture.KAKAO.create());
                SignupRequest request = new SignupRequest("kakao", SOCIAL_TOKEN);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/signup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isConflict());
            }
        }

        @Nested
        @DisplayName("소셜 계정이 이메일이나 닉네임을 제공하지 않은 경우")
        class WhenSocialRequiredInfoMissing {

            @Test
            @DisplayName("400과 SOCIAL_REQUIRED_INFO_MISSING 코드를 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                given(kakaoSocialClient.authenticate(SOCIAL_TOKEN))
                        .willThrow(new SocialRequiredInfoMissingException());
                SignupRequest request = new SignupRequest("kakao", SOCIAL_TOKEN);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/signup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(
                                                AuthErrorCode.SOCIAL_REQUIRED_INFO_MISSING
                                                        .getCode()));
            }
        }

        @Nested
        @DisplayName("지원하지 않는 provider인 경우")
        class WhenProviderUnsupported {

            @Test
            @DisplayName("400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                SignupRequest request = new SignupRequest("naver", SOCIAL_TOKEN);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/signup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }
        }

        @Nested
        @DisplayName("provider 또는 token이 빈 문자열인 경우")
        class WhenRequestFieldBlank {

            @Test
            @DisplayName("provider가 비어 있으면 400을 반환한다")
            void shouldReturnBadRequestWhenProviderBlank() throws Exception {
                // given
                SignupRequest request = new SignupRequest("", SOCIAL_TOKEN);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/signup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("token이 비어 있으면 400을 반환한다")
            void shouldReturnBadRequestWhenTokenBlank() throws Exception {
                // given
                SignupRequest request = new SignupRequest("kakao", "");

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/signup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }
        }

        @Nested
        @DisplayName("provider 또는 token이 길이 상한을 초과한 경우")
        class WhenRequestFieldTooLong {

            @Test
            @DisplayName("provider가 20자를 초과하면 400을 반환한다")
            void shouldReturnBadRequestWhenProviderTooLong() throws Exception {
                // given
                SignupRequest request = new SignupRequest("a".repeat(21), SOCIAL_TOKEN);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/signup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("token이 4096자를 초과하면 400을 반환한다")
            void shouldReturnBadRequestWhenTokenTooLong() throws Exception {
                // given
                SignupRequest request = new SignupRequest("kakao", "a".repeat(4097));

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/signup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login은")
    class Login {

        @Nested
        @DisplayName("가입된 소셜 계정인 경우")
        class WhenMemberRegistered {

            @Test
            @DisplayName("200과 함께 토큰을 반환한다")
            void shouldReturnToken() throws Exception {
                // given
                memberHelper.save(MemberFixture.KAKAO);
                given(kakaoSocialClient.authenticate(SOCIAL_TOKEN))
                        .willReturn(SocialIdentityFixture.KAKAO.create());
                LoginRequest request = new LoginRequest("kakao", SOCIAL_TOKEN);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.accessToken").exists())
                        .andExpect(jsonPath("$.data.refreshToken").exists())
                        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
            }
        }

        @Nested
        @DisplayName("가입되지 않은 소셜 계정인 경우")
        class WhenMemberNotRegistered {

            @Test
            @DisplayName("404를 반환한다")
            void shouldReturnNotFound() throws Exception {
                // given
                given(kakaoSocialClient.authenticate(SOCIAL_TOKEN))
                        .willReturn(SocialIdentityFixture.KAKAO.create());
                LoginRequest request = new LoginRequest("kakao", SOCIAL_TOKEN);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isNotFound());
            }
        }

        @Nested
        @DisplayName("만료되었거나 유효하지 않은 소셜 토큰인 경우")
        class WhenSocialTokenIsInvalid {

            @Test
            @DisplayName("400과 INVALID_SOCIAL_TOKEN 코드를 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                given(kakaoSocialClient.authenticate(SOCIAL_TOKEN))
                        .willThrow(new InvalidSocialTokenException());
                LoginRequest request = new LoginRequest("kakao", SOCIAL_TOKEN);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(AuthErrorCode.INVALID_SOCIAL_TOKEN.getCode()));
            }
        }

        @Nested
        @DisplayName("지원하지 않는 provider인 경우")
        class WhenProviderUnsupported {

            @Test
            @DisplayName("400을 반환한다")
            void shouldReturnBadRequest() throws Exception {
                // given
                LoginRequest request = new LoginRequest("naver", SOCIAL_TOKEN);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }
        }

        @Nested
        @DisplayName("provider 또는 token이 빈 문자열인 경우")
        class WhenRequestFieldBlank {

            @Test
            @DisplayName("provider가 비어 있으면 400을 반환한다")
            void shouldReturnBadRequestWhenProviderBlank() throws Exception {
                // given
                LoginRequest request = new LoginRequest("", SOCIAL_TOKEN);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("token이 비어 있으면 400을 반환한다")
            void shouldReturnBadRequestWhenTokenBlank() throws Exception {
                // given
                LoginRequest request = new LoginRequest("kakao", "");

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }
        }

        @Nested
        @DisplayName("provider 또는 token이 길이 상한을 초과한 경우")
        class WhenRequestFieldTooLong {

            @Test
            @DisplayName("provider가 20자를 초과하면 400을 반환한다")
            void shouldReturnBadRequestWhenProviderTooLong() throws Exception {
                // given
                LoginRequest request = new LoginRequest("a".repeat(21), SOCIAL_TOKEN);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("token이 4096자를 초과하면 400을 반환한다")
            void shouldReturnBadRequestWhenTokenTooLong() throws Exception {
                // given
                LoginRequest request = new LoginRequest("kakao", "a".repeat(4097));

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh는")
    class Refresh {

        @Nested
        @DisplayName("유효한 리프레시 토큰인 경우")
        class WhenRefreshTokenExists {

            @Test
            @DisplayName("새로운 액세스 토큰과 새 리프레시 토큰을 본문으로 반환한다")
            void shouldReturnAccessTokenAndRotatedRefreshToken() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String refreshToken = authHelper.issueRefreshTokenFor(member);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/refresh")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        new RefreshRequest(refreshToken))))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.accessToken").exists())
                        .andExpect(jsonPath("$.data.refreshToken").exists())
                        .andExpect(
                                jsonPath("$.data.refreshToken").value(not(equalTo(refreshToken))))
                        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
            }
        }

        @Nested
        @DisplayName("같은 회원의 다른 기기가 재발급한 뒤인 경우")
        class WhenOtherDeviceRefreshed {

            @Test
            @DisplayName("남아 있는 리프레시 토큰으로도 재발급된다")
            void shouldRefreshWithRemainingToken() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String deviceAToken = authHelper.issueRefreshTokenFor(member);
                String deviceBToken = authHelper.issueRefreshTokenFor(member);
                mockMvc.perform(
                                post("/api/v1/auth/refresh")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        new RefreshRequest(deviceAToken))))
                        .andExpect(status().isOk());

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/refresh")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        new RefreshRequest(deviceBToken))))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.accessToken").exists());
            }
        }

        @Nested
        @DisplayName("리프레시 토큰이 비어 있거나 본문이 없는 경우")
        class WhenRefreshTokenMissing {

            @Test
            @DisplayName("리프레시 토큰이 빈 문자열이면 400을 반환한다")
            void shouldReturnBadRequestWhenRefreshTokenBlank() throws Exception {
                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/refresh")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        new RefreshRequest(""))))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("본문이 없으면 400을 반환한다")
            void shouldReturnBadRequestWhenBodyMissing() throws Exception {
                // when & then
                mockMvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isBadRequest());
            }
        }

        @Nested
        @DisplayName("리프레시 토큰이 저장된 토큰과 일치하지 않는 경우")
        class WhenRefreshTokenUnknown {

            @Test
            @DisplayName("401을 반환한다")
            void shouldReturnUnauthorized() throws Exception {
                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/refresh")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        new RefreshRequest(
                                                                "unknown-refresh-token"))))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout는")
    class Logout {

        @Nested
        @DisplayName("리프레시 토큰과 유효한 액세스 토큰이 있는 경우")
        class WhenTokensAreValid {

            @Test
            @DisplayName("이 기기의 리프레시 토큰을 지우고 액세스 토큰을 블랙리스트에 등록한다")
            void shouldRevokeTokens() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String refreshToken = authHelper.issueRefreshTokenFor(member);
                String accessToken =
                        authHelper.generateAccessToken(
                                String.valueOf(member.getId()), member.getRole().name());

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/logout")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        new LogoutRequest(refreshToken)))
                                        .header(
                                                HttpHeaders.AUTHORIZATION,
                                                authHelper.bearerHeader(accessToken)))
                        .andExpect(status().isNoContent())
                        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
                assertThat(refreshTokenRepository.findMemberIdByValue(refreshToken)).isEmpty();
                assertThat(blacklistAccessTokenRepository.isBlacklisted(accessToken)).isTrue();
            }
        }

        @Nested
        @DisplayName("같은 회원이 다른 기기에도 로그인한 경우")
        class WhenOtherDeviceLoggedIn {

            @Test
            @DisplayName("다른 기기의 리프레시 토큰은 유지한다")
            void shouldKeepOtherDeviceRefreshToken() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String deviceAToken = authHelper.issueRefreshTokenFor(member);
                String deviceBToken = authHelper.issueRefreshTokenFor(member);

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/logout")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        new LogoutRequest(deviceAToken))))
                        .andExpect(status().isNoContent());
                assertThat(refreshTokenRepository.findMemberIdByValue(deviceAToken)).isEmpty();
                assertThat(refreshTokenRepository.findMemberIdByValue(deviceBToken))
                        .contains(String.valueOf(member.getId()));
            }
        }

        @Nested
        @DisplayName("액세스 토큰이 만료된 경우")
        class WhenAccessTokenExpired {

            @Test
            @DisplayName("리프레시 토큰을 지우고 204를 반환한다")
            void shouldRevokeRefreshTokenAndReturnNoContent() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String refreshToken = authHelper.issueRefreshTokenFor(member);
                JwtAccessTokenProvider expiredTokenProvider =
                        new JwtAccessTokenProvider(
                                new AccessTokenProperties(accessTokenProperties.secret(), -1L));
                String expiredAccessToken =
                        expiredTokenProvider.generate(
                                String.valueOf(member.getId()), member.getRole().name());

                // when & then
                mockMvc.perform(
                                post("/api/v1/auth/logout")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        new LogoutRequest(refreshToken)))
                                        .header(
                                                HttpHeaders.AUTHORIZATION,
                                                authHelper.bearerHeader(expiredAccessToken)))
                        .andExpect(status().isNoContent());
                assertThat(refreshTokenRepository.findMemberIdByValue(refreshToken)).isEmpty();
            }
        }

        @Nested
        @DisplayName("토큰이 모두 없는 경우")
        class WhenTokensAreMissing {

            @Test
            @DisplayName("본문이 없어도 204를 반환한다")
            void shouldReturnNoContent() throws Exception {
                // when & then
                mockMvc.perform(post("/api/v1/auth/logout")).andExpect(status().isNoContent());
            }
        }
    }
}
