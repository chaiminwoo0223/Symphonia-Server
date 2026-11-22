package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.auth.application.dto.result.OAuthMemberResult;
import com.symphonia.auth.application.usecase.VerifySocialTokenUseCase;
import com.symphonia.auth.domain.exception.InvalidSocialTokenException;
import com.symphonia.gathering.application.dto.command.CorrectParticipantByGoogleSubjectCommand;
import com.symphonia.gathering.application.dto.command.CorrectParticipantCommand;
import com.symphonia.gathering.application.usecase.CorrectParticipantByGoogleSubjectUseCase;
import com.symphonia.member.domain.entity.SocialProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("CorrectParticipantService 단위 테스트")
class CorrectParticipantServiceTest extends UnitTest {

    private static final String JOIN_TOKEN = "join-token";
    private static final String GOOGLE_ID_TOKEN = "google-id-token";
    private static final String GOOGLE_SUBJECT = "109876543210987654321";
    private static final String NAME = "김영수";
    private static final String PHONE_NUMBER = "010-7777-8888";

    @InjectMocks private CorrectParticipantService correctParticipantService;

    @Mock private VerifySocialTokenUseCase verifySocialTokenUseCase;

    @Mock private CorrectParticipantByGoogleSubjectUseCase correctParticipantByGoogleSubjectUseCase;

    @Nested
    @DisplayName("correct 메서드는")
    class Correct {

        @Nested
        @DisplayName("구글 ID 토큰이 유효한 경우")
        class WhenGoogleIdTokenValid {

            @Test
            @DisplayName("구글로 검증한 sub를 담아 참여 토큰, 이름, 휴대폰과 함께 정정을 맡긴다")
            void shouldCorrectWithVerifiedSubject() {
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
                correctParticipantService.correct(command());

                // then
                then(correctParticipantByGoogleSubjectUseCase)
                        .should()
                        .correctByGoogleSubject(
                                new CorrectParticipantByGoogleSubjectCommand(
                                        JOIN_TOKEN, GOOGLE_SUBJECT, NAME, PHONE_NUMBER));
            }
        }

        @Nested
        @DisplayName("구글 ID 토큰이 유효하지 않은 경우")
        class WhenGoogleIdTokenInvalid {

            @Test
            @DisplayName("예외를 그대로 던지고 정정을 맡기지 않는다")
            void shouldThrowInvalidSocialTokenException() {
                // given
                given(verifySocialTokenUseCase.verify("google", GOOGLE_ID_TOKEN))
                        .willThrow(new InvalidSocialTokenException());

                // when & then
                assertThatThrownBy(() -> correctParticipantService.correct(command()))
                        .isInstanceOf(InvalidSocialTokenException.class);
                then(correctParticipantByGoogleSubjectUseCase).shouldHaveNoInteractions();
            }
        }

        private CorrectParticipantCommand command() {
            return new CorrectParticipantCommand(JOIN_TOKEN, GOOGLE_ID_TOKEN, NAME, PHONE_NUMBER);
        }
    }
}
