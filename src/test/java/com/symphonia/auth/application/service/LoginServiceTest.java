package com.symphonia.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.auth.application.dto.command.LoginCommand;
import com.symphonia.auth.application.dto.result.OAuthMemberResult;
import com.symphonia.auth.application.dto.result.TokenResult;
import com.symphonia.auth.application.usecase.ExchangeSocialCodeUseCase;
import com.symphonia.auth.application.usecase.IssueTokenUseCase;
import com.symphonia.auth.fixture.SocialIdentityFixture;
import com.symphonia.member.application.dto.result.MemberResult;
import com.symphonia.member.application.usecase.GetMemberUseCase;
import com.symphonia.member.domain.entity.Role;
import com.symphonia.member.domain.entity.SocialProvider;
import com.symphonia.member.domain.error.MemberErrorCode;
import com.symphonia.member.domain.exception.MemberNotFoundException;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.fixture.MemberResultFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("LoginService 단위 테스트")
class LoginServiceTest extends UnitTest {

    private static final String PROVIDER = "kakao";
    private static final String CODE = "auth-code";
    private static final String IP = "127.0.0.1";
    private static final Long MEMBER_ID = 1L;
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";

    @InjectMocks private LoginService loginService;

    @Mock private ExchangeSocialCodeUseCase exchangeSocialCodeUseCase;

    @Mock private GetMemberUseCase getMemberUseCase;

    @Mock private IssueTokenUseCase issueTokenUseCase;

    @Nested
    @DisplayName("login 메서드는")
    class Login {
        private final LoginCommand command = new LoginCommand(PROVIDER, CODE, IP);
        private final OAuthMemberResult oAuthMemberResult =
                OAuthMemberResult.from(SocialIdentityFixture.KAKAO.create());

        @BeforeEach
        void setUp() {
            given(exchangeSocialCodeUseCase.exchange(PROVIDER, CODE)).willReturn(oAuthMemberResult);
        }

        @Nested
        @DisplayName("소셜 계정으로 가입된 멤버가 존재하는 경우")
        class WhenMemberExists {
            private final MemberResult memberResult =
                    new MemberResultFixture(MemberFixture.KAKAO).id(MEMBER_ID).build();

            @BeforeEach
            void setUp() {
                given(
                                getMemberUseCase.getBySocialLogin(
                                        SocialProvider.KAKAO, oAuthMemberResult.socialId()))
                        .willReturn(memberResult);
                given(issueTokenUseCase.issue(String.valueOf(MEMBER_ID), Role.ROLE_MEMBER.name()))
                        .willReturn(
                                TokenResult.of(
                                        ACCESS_TOKEN, REFRESH_TOKEN, String.valueOf(MEMBER_ID)));
            }

            @Test
            @DisplayName("OAuthMemberResult의 socialProvider와 socialId로 멤버를 조회한다.")
            void shouldGetMemberBySocialLogin() {
                // when
                loginService.login(command);

                // then
                then(getMemberUseCase)
                        .should()
                        .getBySocialLogin(SocialProvider.KAKAO, oAuthMemberResult.socialId());
            }

            @Test
            @DisplayName("조회된 멤버 ID와 역할로 토큰을 발급해 반환한다.")
            void shouldIssueAndReturnToken() {
                // when
                TokenResult result = loginService.login(command);

                // then
                then(issueTokenUseCase)
                        .should()
                        .issue(String.valueOf(MEMBER_ID), Role.ROLE_MEMBER.name());
                assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
                assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
            }
        }

        @Nested
        @DisplayName("소셜 계정으로 가입된 멤버가 없는 경우")
        class WhenMemberNotFound {

            @Test
            @DisplayName("예외가 그대로 전파된다.")
            void shouldPropagateException() {
                // given
                given(
                                getMemberUseCase.getBySocialLogin(
                                        SocialProvider.KAKAO, oAuthMemberResult.socialId()))
                        .willThrow(new MemberNotFoundException());

                // when & then
                assertThatThrownBy(() -> loginService.login(command))
                        .isInstanceOf(MemberNotFoundException.class)
                        .hasMessage(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
                then(issueTokenUseCase).shouldHaveNoInteractions();
            }
        }
    }
}
