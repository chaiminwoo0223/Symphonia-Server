package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.auth.application.dto.result.OAuthMemberResult;
import com.symphonia.auth.application.usecase.VerifySocialTokenUseCase;
import com.symphonia.auth.domain.exception.InvalidSocialTokenException;
import com.symphonia.gathering.application.dto.command.RemoveParticipantBySubjectCommand;
import com.symphonia.gathering.application.dto.command.WithdrawParticipantCommand;
import com.symphonia.gathering.application.usecase.RemoveParticipantBySubjectUseCase;
import com.symphonia.member.domain.entity.SocialProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("WithdrawParticipantService 단위 테스트")
class WithdrawParticipantServiceTest extends UnitTest {

    private static final String JOIN_TOKEN = "join-token";
    private static final String GOOGLE_ID_TOKEN = "google-id-token";
    private static final String GOOGLE_SUBJECT = "109876543210987654321";

    @InjectMocks private WithdrawParticipantService withdrawParticipantService;

    @Mock private VerifySocialTokenUseCase verifySocialTokenUseCase;
    @Mock private RemoveParticipantBySubjectUseCase removeParticipantBySubjectUseCase;

    @Nested
    @DisplayName("withdraw 메서드는")
    class Withdraw {

        @Nested
        @DisplayName("구글 ID 토큰이 유효한 경우")
        class WhenGoogleIdTokenValid {

            @Test
            @DisplayName("구글로 검증한 sub를 담아 참여 토큰과 함께 철회를 맡긴다")
            void shouldRemoveWithVerifiedSubject() {
                // given
                given(verifySocialTokenUseCase.verify("google", GOOGLE_ID_TOKEN))
                        .willReturn(
                                new OAuthMemberResult(
                                        GOOGLE_SUBJECT,
                                        "김철수",
                                        "kim@example.com",
                                        "https://example.com/kim.png",
                                        SocialProvider.GOOGLE));

                // when
                withdrawParticipantService.withdraw(command());

                // then
                then(removeParticipantBySubjectUseCase)
                        .should()
                        .remove(new RemoveParticipantBySubjectCommand(JOIN_TOKEN, GOOGLE_SUBJECT));
            }
        }

        @Nested
        @DisplayName("구글 ID 토큰이 유효하지 않은 경우")
        class WhenGoogleIdTokenInvalid {

            @Test
            @DisplayName("예외를 그대로 던지고 철회를 맡기지 않는다")
            void shouldThrowInvalidSocialTokenException() {
                // given
                given(verifySocialTokenUseCase.verify("google", GOOGLE_ID_TOKEN))
                        .willThrow(new InvalidSocialTokenException());

                // when & then
                assertThatThrownBy(() -> withdrawParticipantService.withdraw(command()))
                        .isInstanceOf(InvalidSocialTokenException.class);
                then(removeParticipantBySubjectUseCase).shouldHaveNoInteractions();
            }
        }

        private WithdrawParticipantCommand command() {
            return new WithdrawParticipantCommand(JOIN_TOKEN, GOOGLE_ID_TOKEN);
        }
    }
}
