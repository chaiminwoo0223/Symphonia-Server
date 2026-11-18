package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.dto.command.SubmitParticipantPairingFeedbackCommand;
import com.symphonia.gathering.application.dto.result.LinkedParticipantResult;
import com.symphonia.gathering.application.usecase.GetLinkedParticipantUseCase;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.exception.GatheringPairingConflictException;
import com.symphonia.gathering.domain.exception.GatheringPairingNotFoundException;
import com.symphonia.gathering.domain.exception.LinkTokenNotFoundException;
import com.symphonia.gathering.domain.exception.PairingFeedbackAlreadySubmittedException;
import com.symphonia.gathering.domain.exception.SentShareRequiredException;
import com.symphonia.gathering.domain.repository.GatheringPairingRepository;
import com.symphonia.gathering.domain.repository.ParticipantPairingFeedbackRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.ShareStatus;
import com.symphonia.gathering.fixture.GatheringPairingFixture;
import com.symphonia.gathering.fixture.SettlementFixture;
import com.symphonia.global.config.TimeConfig;
import com.symphonia.pairing.application.dto.command.SubmitAnonymousPairingFeedbackCommand;
import com.symphonia.pairing.application.usecase.SubmitAnonymousPairingFeedbackUseCase;
import com.symphonia.pairing.domain.vo.PairingRating;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

@DisplayName("ParticipantPairingFeedbackService 단위 테스트")
class ParticipantPairingFeedbackServiceTest extends UnitTest {

    private static final GatheringPairingFixture PAIRING = GatheringPairingFixture.FRIEND_CASUAL;
    private static final Long PARTICIPANT_ID = 20L;
    private static final Long OTHER_PARTICIPANT_ID = 21L;
    private static final Long GATHERING_ID = 10L;
    private static final Long ROUND_ID = 1L;
    private static final Long SETTLEMENT_ID = 40L;
    private static final String LINK_TOKEN = "link-token";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 18, 0);
    private static final LocalDateTime DELETES_AT = NOW.plusDays(7);

    private ParticipantPairingFeedbackService participantPairingFeedbackService;

    @Mock private GetLinkedParticipantUseCase getLinkedParticipantUseCase;
    @Mock private GatheringPairingRepository gatheringPairingRepository;
    @Mock private SettlementRepository settlementRepository;
    @Mock private ParticipantPairingFeedbackRepository participantPairingFeedbackRepository;
    @Mock private SubmitAnonymousPairingFeedbackUseCase submitAnonymousPairingFeedbackUseCase;

    @BeforeEach
    void setUp() {
        participantPairingFeedbackService =
                new ParticipantPairingFeedbackService(
                        getLinkedParticipantUseCase,
                        gatheringPairingRepository,
                        settlementRepository,
                        participantPairingFeedbackRepository,
                        submitAnonymousPairingFeedbackUseCase,
                        Clock.fixed(
                                NOW.atZone(TimeConfig.SERVICE_ZONE).toInstant(),
                                TimeConfig.SERVICE_ZONE));
    }

    @Nested
    @DisplayName("submit 메서드는")
    class Submit {

        // 토큰이 없거나 모임의 파기 시각이 지났거나 데이터 키를 지운 경우다. 갈래별 판단은 ParticipantQueryServiceTest가 검증한다.
        @Nested
        @DisplayName("개인 링크 토큰으로 참여자를 찾지 못한 경우")
        class WhenLinkTokenNotFound {

            @Test
            @DisplayName("예외를 던지고 제출 표시와 피드백을 저장하지 않는다")
            void shouldThrowLinkTokenNotFoundException() {
                // given
                given(getLinkedParticipantUseCase.getLinkedParticipant(LINK_TOKEN))
                        .willThrow(new LinkTokenNotFoundException());

                // when & then
                assertThatThrownBy(() -> participantPairingFeedbackService.submit(command()))
                        .isInstanceOf(LinkTokenNotFoundException.class);
                then(gatheringPairingRepository).shouldHaveNoInteractions();
                then(participantPairingFeedbackRepository).shouldHaveNoInteractions();
                then(submitAnonymousPairingFeedbackUseCase).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임에서 고른 페어링이 없는 경우")
        class WhenGatheringPairingNotFound {

            @Test
            @DisplayName("예외를 던지고 제출 표시와 피드백을 저장하지 않는다")
            void shouldThrowGatheringPairingNotFoundException() {
                // given
                givenLinkedParticipant();
                given(gatheringPairingRepository.findByParticipantId(PARTICIPANT_ID))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> participantPairingFeedbackService.submit(command()))
                        .isInstanceOf(GatheringPairingNotFoundException.class);
                then(participantPairingFeedbackRepository).shouldHaveNoInteractions();
                then(submitAnonymousPairingFeedbackUseCase).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("참여자가 본 조합이 모임의 페어링과 다른 경우")
        class WhenGatheringPairingChanged {

            @Test
            @DisplayName("예외를 던지고 제출 표시와 피드백을 저장하지 않는다")
            void shouldThrowGatheringPairingConflictException() {
                // given
                givenGatheringPairing();
                SubmitParticipantPairingFeedbackCommand command =
                        new SubmitParticipantPairingFeedbackCommand(
                                LINK_TOKEN,
                                PAIRING.getDrinkId(),
                                PAIRING.getAnjuId() + 1,
                                PairingRating.DISLIKE);

                // when & then
                assertThatThrownBy(() -> participantPairingFeedbackService.submit(command))
                        .isInstanceOf(GatheringPairingConflictException.class);
                then(participantPairingFeedbackRepository).shouldHaveNoInteractions();
                then(submitAnonymousPairingFeedbackUseCase).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("참여한 정산 건이 없는 경우")
        class WhenNoSettlements {

            @Test
            @DisplayName("예외를 던지고 제출 표시와 피드백을 저장하지 않는다")
            void shouldThrowSentShareRequiredException() {
                // given
                givenGatheringPairing();
                givenSettlements(List.of(), List.of());

                // when & then
                assertThatThrownBy(() -> participantPairingFeedbackService.submit(command()))
                        .isInstanceOf(SentShareRequiredException.class);
                thenNothingSubmitted();
            }
        }

        @Nested
        @DisplayName("본인 분담금이 미입금인 경우")
        class WhenShareUnpaid {

            @Test
            @DisplayName("예외를 던지고 제출 표시와 피드백을 저장하지 않는다")
            void shouldThrowSentShareRequiredException() {
                // given
                givenGatheringPairing();
                givenSettlements(
                        List.of(),
                        List.of(
                                SettlementFixture.DINNER.createWithFirstShareStatus(
                                        SETTLEMENT_ID,
                                        GATHERING_ID,
                                        ROUND_ID,
                                        OTHER_PARTICIPANT_ID,
                                        List.of(OTHER_PARTICIPANT_ID, PARTICIPANT_ID),
                                        ShareStatus.UNPAID)));

                // when & then
                assertThatThrownBy(() -> participantPairingFeedbackService.submit(command()))
                        .isInstanceOf(SentShareRequiredException.class);
                thenNothingSubmitted();
            }
        }

        // 0원 분담금은 확인 완료로 시작하지만 돈이 오가지 않는다. 0원으로 지정한 가짜 참여자가 보내지 않고 통과하지 못하게 막는다.
        @Nested
        @DisplayName("본인 분담금이 0원인 경우")
        class WhenShareAmountZero {

            @Test
            @DisplayName("확인 완료로 시작했어도 예외를 던지고 제출 표시와 피드백을 저장하지 않는다")
            void shouldThrowSentShareRequiredException() {
                // given
                givenGatheringPairing();
                givenSettlements(
                        List.of(),
                        List.of(
                                SettlementFixture.DINNER.createWithFixedAmounts(
                                        SETTLEMENT_ID,
                                        GATHERING_ID,
                                        ROUND_ID,
                                        OTHER_PARTICIPANT_ID,
                                        List.of(OTHER_PARTICIPANT_ID, PARTICIPANT_ID),
                                        Map.of(PARTICIPANT_ID, 0L))));

                // when & then
                assertThatThrownBy(() -> participantPairingFeedbackService.submit(command()))
                        .isInstanceOf(SentShareRequiredException.class);
                thenNothingSubmitted();
            }
        }

        @Nested
        @DisplayName("결제자가 다른 참여자의 분담금을 받지 못한 경우")
        class WhenPayerReceivedNothing {

            @Test
            @DisplayName("예외를 던지고 제출 표시와 피드백을 저장하지 않는다")
            void shouldThrowSentShareRequiredException() {
                // given
                givenGatheringPairing();
                givenSettlements(
                        List.of(
                                SettlementFixture.DINNER.createWithId(
                                        SETTLEMENT_ID,
                                        GATHERING_ID,
                                        ROUND_ID,
                                        PARTICIPANT_ID,
                                        List.of(PARTICIPANT_ID, OTHER_PARTICIPANT_ID))),
                        List.of());

                // when & then
                assertThatThrownBy(() -> participantPairingFeedbackService.submit(command()))
                        .isInstanceOf(SentShareRequiredException.class);
                thenNothingSubmitted();
            }
        }

        // 다시 낸 요청과 동시에 낸 요청 중 나중 요청이 모두 이 경우다. 동시 제출에서 한 요청만 표시를 넣는지는 Repository 테스트가 검증한다.
        @Nested
        @DisplayName("이미 피드백을 낸 참여자인 경우")
        class WhenFeedbackAlreadySubmitted {

            @Test
            @DisplayName("예외를 던지고 피드백을 다시 저장하지 않는다")
            void shouldThrowPairingFeedbackAlreadySubmittedException() {
                // given
                givenGatheringPairing();
                givenSentShare();
                given(
                                participantPairingFeedbackRepository.markSubmittedIfAbsent(
                                        PARTICIPANT_ID, NOW))
                        .willReturn(false);

                // when & then
                assertThatThrownBy(() -> participantPairingFeedbackService.submit(command()))
                        .isInstanceOf(PairingFeedbackAlreadySubmittedException.class);
                then(submitAnonymousPairingFeedbackUseCase).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("피드백을 낼 수 있는 경우")
        class WhenSubmittable {

            @Test
            @DisplayName("현재 시각으로 제출 표시를 남긴 뒤 모임 페어링의 관계, 분위기와 요청한 평점으로 익명 피드백을 낸다")
            void shouldMarkSubmittedAndSubmitAnonymousFeedback() {
                // given
                givenGatheringPairing();
                givenSentShare();
                given(
                                participantPairingFeedbackRepository.markSubmittedIfAbsent(
                                        PARTICIPANT_ID, NOW))
                        .willReturn(true);

                // when
                participantPairingFeedbackService.submit(command());

                // then
                InOrder inOrder =
                        inOrder(
                                participantPairingFeedbackRepository,
                                submitAnonymousPairingFeedbackUseCase);
                inOrder.verify(participantPairingFeedbackRepository)
                        .markSubmittedIfAbsent(PARTICIPANT_ID, NOW);
                inOrder.verify(submitAnonymousPairingFeedbackUseCase)
                        .submitAnonymous(
                                SubmitAnonymousPairingFeedbackCommand.of(
                                        PAIRING.getDrinkId(),
                                        PAIRING.getAnjuId(),
                                        PAIRING.getRelationshipType(),
                                        PAIRING.getMoodType(),
                                        PairingRating.DISLIKE));
            }
        }

        @Nested
        @DisplayName("결제자가 다른 참여자의 분담금을 받은 경우")
        class WhenPayerReceivedShare {

            @Test
            @DisplayName("피드백을 낼 수 있다")
            void shouldSubmitAnonymousFeedback() {
                // given
                givenGatheringPairing();
                given(settlementRepository.findAllByPayerParticipantId(PARTICIPANT_ID))
                        .willReturn(
                                List.of(
                                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                                SETTLEMENT_ID,
                                                GATHERING_ID,
                                                ROUND_ID,
                                                PARTICIPANT_ID,
                                                List.of(PARTICIPANT_ID, OTHER_PARTICIPANT_ID),
                                                ShareStatus.CONFIRMED)));
                given(settlementRepository.findAllByShareParticipantId(PARTICIPANT_ID))
                        .willReturn(List.of());
                given(
                                participantPairingFeedbackRepository.markSubmittedIfAbsent(
                                        PARTICIPANT_ID, NOW))
                        .willReturn(true);

                // when
                participantPairingFeedbackService.submit(command());

                // then
                then(submitAnonymousPairingFeedbackUseCase)
                        .should()
                        .submitAnonymous(
                                SubmitAnonymousPairingFeedbackCommand.of(
                                        PAIRING.getDrinkId(),
                                        PAIRING.getAnjuId(),
                                        PAIRING.getRelationshipType(),
                                        PAIRING.getMoodType(),
                                        PairingRating.DISLIKE));
            }
        }

        private SubmitParticipantPairingFeedbackCommand command() {
            return new SubmitParticipantPairingFeedbackCommand(
                    LINK_TOKEN, PAIRING.getDrinkId(), PAIRING.getAnjuId(), PairingRating.DISLIKE);
        }

        private void givenSettlements(
                List<Settlement> payerSettlements, List<Settlement> shareSettlements) {
            given(settlementRepository.findAllByPayerParticipantId(PARTICIPANT_ID))
                    .willReturn(payerSettlements);
            given(settlementRepository.findAllByShareParticipantId(PARTICIPANT_ID))
                    .willReturn(shareSettlements);
        }

        private void thenNothingSubmitted() {
            then(participantPairingFeedbackRepository).shouldHaveNoInteractions();
            then(submitAnonymousPairingFeedbackUseCase).shouldHaveNoInteractions();
        }

        // 다른 참여자가 결제한 정산 건에서 본인 분담금을 보낸 상태다.
        private void givenSentShare() {
            given(settlementRepository.findAllByPayerParticipantId(PARTICIPANT_ID))
                    .willReturn(List.of());
            given(settlementRepository.findAllByShareParticipantId(PARTICIPANT_ID))
                    .willReturn(
                            List.of(
                                    SettlementFixture.DINNER.createWithFirstShareStatus(
                                            SETTLEMENT_ID,
                                            GATHERING_ID,
                                            ROUND_ID,
                                            OTHER_PARTICIPANT_ID,
                                            List.of(OTHER_PARTICIPANT_ID, PARTICIPANT_ID),
                                            ShareStatus.SENT)));
        }

        private void givenGatheringPairing() {
            givenLinkedParticipant();
            given(gatheringPairingRepository.findByParticipantId(PARTICIPANT_ID))
                    .willReturn(Optional.of(PAIRING.create()));
        }

        private void givenLinkedParticipant() {
            given(getLinkedParticipantUseCase.getLinkedParticipant(LINK_TOKEN))
                    .willReturn(new LinkedParticipantResult(PARTICIPANT_ID, DELETES_AT));
        }
    }
}
