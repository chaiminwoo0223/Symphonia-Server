package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.auth.application.dto.result.OAuthMemberResult;
import com.symphonia.auth.application.usecase.VerifySocialTokenUseCase;
import com.symphonia.auth.domain.exception.InvalidSocialTokenException;
import com.symphonia.gathering.application.dto.command.AddParticipantCommand;
import com.symphonia.gathering.application.dto.command.JoinGatheringCommand;
import com.symphonia.gathering.application.dto.result.ParticipantResult;
import com.symphonia.gathering.application.usecase.AddParticipantUseCase;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.member.domain.entity.SocialProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("JoinGatheringService 단위 테스트")
class JoinGatheringServiceTest extends UnitTest {

    private static final Long PARTICIPANT_ID = 20L;
    private static final String JOIN_TOKEN = "join-token";
    private static final String GOOGLE_ID_TOKEN = "google-id-token";
    private static final String GOOGLE_SUBJECT = "109876543210987654321";
    private static final String HYPHENATED_PHONE_NUMBER = "010-1234-5678";
    private static final String LINK_TOKEN = "link-token";

    @InjectMocks private JoinGatheringService joinGatheringService;

    @Mock private VerifySocialTokenUseCase verifySocialTokenUseCase;
    @Mock private AddParticipantUseCase addParticipantUseCase;

    @Nested
    @DisplayName("join 메서드는")
    class Join {

        @Nested
        @DisplayName("구글 ID 토큰이 유효한 경우")
        class WhenGoogleIdTokenValid {

            @Test
            @DisplayName("구글로 검증한 sub를 담아 참여를 맡기고 그 결과를 반환한다")
            void shouldJoinWithVerifiedSubject() {
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
                given(addParticipantUseCase.add(addParticipantCommand())).willReturn(expected);

                // when
                ParticipantResult result = joinGatheringService.join(command());

                // then
                assertThat(result).isEqualTo(expected);
            }
        }

        @Nested
        @DisplayName("구글 ID 토큰이 유효하지 않은 경우")
        class WhenGoogleIdTokenInvalid {

            @Test
            @DisplayName("예외를 던지고 참여를 맡기지 않는다")
            void shouldThrowInvalidSocialTokenException() {
                // given
                given(verifySocialTokenUseCase.verify("google", GOOGLE_ID_TOKEN))
                        .willThrow(new InvalidSocialTokenException());

                // when & then
                assertThatThrownBy(() -> joinGatheringService.join(command()))
                        .isInstanceOf(InvalidSocialTokenException.class);
                then(addParticipantUseCase).shouldHaveNoInteractions();
            }
        }

        private JoinGatheringCommand command() {
            return new JoinGatheringCommand(
                    JOIN_TOKEN,
                    GOOGLE_ID_TOKEN,
                    ParticipantFixture.KIM.getName(),
                    HYPHENATED_PHONE_NUMBER,
                    true,
                    GatheringPolicy.CONSENT_VERSION);
        }

        private AddParticipantCommand addParticipantCommand() {
            return new AddParticipantCommand(
                    JOIN_TOKEN,
                    GOOGLE_SUBJECT,
                    ParticipantFixture.KIM.getName(),
                    HYPHENATED_PHONE_NUMBER,
                    true,
                    GatheringPolicy.CONSENT_VERSION);
        }
    }
}
