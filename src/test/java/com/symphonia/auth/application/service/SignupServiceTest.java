package com.symphonia.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.auth.application.dto.command.SignupCommand;
import com.symphonia.auth.application.dto.result.OAuthMemberResult;
import com.symphonia.auth.application.dto.result.TokenResult;
import com.symphonia.auth.application.usecase.ExchangeSocialCodeUseCase;
import com.symphonia.auth.application.usecase.IssueTokenUseCase;
import com.symphonia.auth.fixture.SocialIdentityFixture;
import com.symphonia.member.application.dto.command.CreateMemberCommand;
import com.symphonia.member.application.dto.result.MemberResult;
import com.symphonia.member.application.usecase.CreateMemberUseCase;
import com.symphonia.member.domain.entity.Role;
import com.symphonia.member.domain.error.MemberErrorCode;
import com.symphonia.member.domain.exception.MemberAlreadyExistsException;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.fixture.MemberResultFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("SignupService 단위 테스트")
class SignupServiceTest extends UnitTest {

    private static final String PROVIDER = "kakao";
    private static final String CODE = "auth-code";
    private static final String IP = "127.0.0.1";
    private static final Long MEMBER_ID = 1L;
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";

    @InjectMocks private SignupService signupService;

    @Mock private ExchangeSocialCodeUseCase exchangeSocialCodeUseCase;

    @Mock private CreateMemberUseCase createMemberUseCase;

    @Mock private IssueTokenUseCase issueTokenUseCase;

    @Nested
    @DisplayName("signup 메서드는")
    class Signup {
        private final SignupCommand command = new SignupCommand(PROVIDER, CODE, IP);
        private final OAuthMemberResult oAuthMemberResult =
                OAuthMemberResult.from(SocialIdentityFixture.KAKAO.create());

        @BeforeEach
        void setUp() {
            given(exchangeSocialCodeUseCase.exchange(PROVIDER, CODE)).willReturn(oAuthMemberResult);
        }

        @Nested
        @DisplayName("소셜 계정이 아직 가입되지 않은 경우")
        class WhenMemberNotExists {
            private final MemberResult memberResult =
                    new MemberResultFixture(MemberFixture.KAKAO).id(MEMBER_ID).build();

            @BeforeEach
            void setUp() {
                given(createMemberUseCase.create(any(CreateMemberCommand.class)))
                        .willReturn(memberResult);
                given(issueTokenUseCase.issue(String.valueOf(MEMBER_ID), Role.ROLE_MEMBER.name()))
                        .willReturn(
                                TokenResult.of(
                                        ACCESS_TOKEN, REFRESH_TOKEN, String.valueOf(MEMBER_ID)));
            }

            @Test
            @DisplayName("OAuthMemberResult로 구성한 CreateMemberCommand로 멤버 생성을 요청한다.")
            void shouldCreateMemberWithMappedCommand() {
                // when
                signupService.signup(command);

                // then
                ArgumentCaptor<CreateMemberCommand> captor =
                        ArgumentCaptor.forClass(CreateMemberCommand.class);
                then(createMemberUseCase).should().create(captor.capture());
                CreateMemberCommand captured = captor.getValue();
                assertThat(captured.socialId()).isEqualTo(oAuthMemberResult.socialId());
                assertThat(captured.nickname()).isEqualTo(oAuthMemberResult.nickname());
                assertThat(captured.email()).isEqualTo(oAuthMemberResult.email());
                assertThat(captured.profileImage()).isEqualTo(oAuthMemberResult.profileImage());
                assertThat(captured.socialProvider()).isEqualTo(oAuthMemberResult.socialProvider());
            }

            @Test
            @DisplayName("생성된 멤버 ID와 역할로 토큰을 발급해 반환한다.")
            void shouldIssueAndReturnToken() {
                // when
                TokenResult result = signupService.signup(command);

                // then
                then(issueTokenUseCase)
                        .should()
                        .issue(String.valueOf(MEMBER_ID), Role.ROLE_MEMBER.name());
                assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
                assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
            }
        }

        @Nested
        @DisplayName("이미 가입된 소셜 계정인 경우")
        class WhenMemberAlreadyExists {

            @Test
            @DisplayName("예외가 그대로 전파된다.")
            void shouldPropagateMemberAlreadyExistsException() {
                // given
                given(createMemberUseCase.create(any(CreateMemberCommand.class)))
                        .willThrow(new MemberAlreadyExistsException());

                // when & then
                assertThatThrownBy(() -> signupService.signup(command))
                        .isInstanceOf(MemberAlreadyExistsException.class)
                        .hasMessage(MemberErrorCode.MEMBER_ALREADY_EXISTS.getMessage());
                then(issueTokenUseCase).shouldHaveNoInteractions();
            }
        }
    }
}
