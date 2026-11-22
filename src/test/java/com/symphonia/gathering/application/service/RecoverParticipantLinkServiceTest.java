package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.auth.application.dto.result.OAuthMemberResult;
import com.symphonia.auth.application.usecase.VerifySocialTokenUseCase;
import com.symphonia.auth.domain.exception.InvalidSocialTokenException;
import com.symphonia.gathering.application.dto.command.RecoverParticipantLinkCommand;
import com.symphonia.gathering.application.dto.command.ReissueParticipantLinkTokenByGoogleSubjectCommand;
import com.symphonia.gathering.application.dto.result.ParticipantResult;
import com.symphonia.gathering.application.usecase.ReissueParticipantLinkTokenByGoogleSubjectUseCase;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.member.domain.entity.SocialProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("RecoverParticipantLinkService 단위 테스트")
class RecoverParticipantLinkServiceTest extends UnitTest {

    private static final Long PARTICIPANT_ID = 20L;
    private static final String JOIN_TOKEN = "join-token";
    private static final String GOOGLE_ID_TOKEN = "google-id-token";
    private static final String GOOGLE_SUBJECT = "109876543210987654321";
    private static final String LINK_TOKEN = "link-token";

    @InjectMocks private RecoverParticipantLinkService recoverParticipantLinkService;

    @Mock private VerifySocialTokenUseCase verifySocialTokenUseCase;

    @Mock
    private ReissueParticipantLinkTokenByGoogleSubjectUseCase
            reissueParticipantLinkTokenByGoogleSubjectUseCase;

    @Nested
    @DisplayName("recover 메서드는")
    class Recover {

        @Nested
        @DisplayName("구글 ID 토큰이 유효한 경우")
        class WhenGoogleIdTokenValid {

            @Test
            @DisplayName("구글로 검증한 sub를 담아 재발급을 맡기고 그 결과를 반환한다")
            void shouldReissueWithVerifiedSubject() {
                // given
                given(verifySocialTokenUseCase.verify("google", GOOGLE_ID_TOKEN))
                        .willReturn(
                                new OAuthMemberResult(
                                        GOOGLE_SUBJECT,
                                        "김철수",
                                        "kim@example.com",
                                        "https://example.com/kim.png",
                                        SocialProvider.GOOGLE));
                ParticipantResult expected =
                        ParticipantResult.of(
                                ParticipantFixture.KIM.createWithId(PARTICIPANT_ID), LINK_TOKEN);
                given(
                                reissueParticipantLinkTokenByGoogleSubjectUseCase
                                        .reissueByGoogleSubject(reissueCommand()))
                        .willReturn(expected);

                // when
                ParticipantResult result = recoverParticipantLinkService.recover(command());

                // then
                assertThat(result).isEqualTo(expected);
            }
        }

        @Nested
        @DisplayName("구글 ID 토큰이 유효하지 않은 경우")
        class WhenGoogleIdTokenInvalid {

            @Test
            @DisplayName("예외를 던지고 재발급을 맡기지 않는다")
            void shouldThrowInvalidSocialTokenException() {
                // given
                given(verifySocialTokenUseCase.verify("google", GOOGLE_ID_TOKEN))
                        .willThrow(new InvalidSocialTokenException());

                // when & then
                assertThatThrownBy(() -> recoverParticipantLinkService.recover(command()))
                        .isInstanceOf(InvalidSocialTokenException.class);
                then(reissueParticipantLinkTokenByGoogleSubjectUseCase).shouldHaveNoInteractions();
            }
        }

        private RecoverParticipantLinkCommand command() {
            return new RecoverParticipantLinkCommand(JOIN_TOKEN, GOOGLE_ID_TOKEN);
        }

        private ReissueParticipantLinkTokenByGoogleSubjectCommand reissueCommand() {
            return new ReissueParticipantLinkTokenByGoogleSubjectCommand(
                    JOIN_TOKEN, GOOGLE_SUBJECT);
        }
    }
}
