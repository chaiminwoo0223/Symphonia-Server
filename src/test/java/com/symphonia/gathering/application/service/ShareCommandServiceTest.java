package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.dto.command.ConfirmShareCommand;
import com.symphonia.gathering.application.dto.command.MarkSharesSentCommand;
import com.symphonia.gathering.application.dto.command.RevertShareCommand;
import com.symphonia.gathering.application.dto.result.LinkedParticipantResult;
import com.symphonia.gathering.application.dto.result.PayerSharesResult;
import com.symphonia.gathering.application.dto.result.SettlementResult;
import com.symphonia.gathering.application.dto.result.SettlementShareResult;
import com.symphonia.gathering.application.dto.result.ShareBreakdownResult;
import com.symphonia.gathering.application.event.SharesSentEvent;
import com.symphonia.gathering.application.usecase.GetLinkedParticipantUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.exception.LinkTokenNotFoundException;
import com.symphonia.gathering.domain.exception.NotSettlementPayerException;
import com.symphonia.gathering.domain.exception.SettlementAlreadyCancelledException;
import com.symphonia.gathering.domain.exception.SettlementShareNotFoundException;
import com.symphonia.gathering.domain.exception.ZeroShareStatusUnchangeableException;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.ShareStatus;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.GatheringRoundFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.fixture.SettlementFixture;
import com.symphonia.global.config.TimeConfig;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

@DisplayName("ShareCommandService 단위 테스트")
class ShareCommandServiceTest extends UnitTest {

    private static final Long GATHERING_ID = 10L;
    private static final Long ROUND_ID = GatheringRoundFixture.FIRST.getId();
    private static final Long SECOND_ROUND_ID = GatheringRoundFixture.SECOND.getId();
    private static final Long SETTLEMENT_ID = 30L;
    private static final Long SECOND_SETTLEMENT_ID = 31L;
    private static final Long THIRD_SETTLEMENT_ID = 32L;
    private static final Long PAYER_ID = 20L;
    private static final Long LEE_ID = 21L;
    private static final Long PARK_ID = 22L;
    private static final Long OUTSIDER_ID = 99L;
    private static final List<Long> PARTICIPANT_IDS = List.of(PAYER_ID, LEE_ID, PARK_ID);
    private static final String LINK_TOKEN = "link-token";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 18, 0);
    private static final LocalDateTime DELETES_AT = NOW.plusDays(7);
    private static final LocalDateTime OPENED_AT = NOW.minusHours(1);
    private static final LocalDateTime STATUS_CHANGED_AT = NOW.minusMinutes(30);

    private ShareCommandService shareCommandService;

    @Mock private GetLinkedParticipantUseCase getLinkedParticipantUseCase;
    @Mock private GatheringRepository gatheringRepository;
    @Mock private SettlementRepository settlementRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        givenNow(NOW);
    }

    @Nested
    @DisplayName("markSent 메서드는")
    class MarkSent {

        // 토큰이 없거나 모임의 파기 시각이 지났거나 데이터 키를 지운 경우다. 갈래별 판단은 ParticipantQueryServiceTest가 검증한다.
        @Nested
        @DisplayName("개인 링크 토큰으로 참여자를 찾지 못한 경우")
        class WhenLinkTokenNotFound {

            @Test
            @DisplayName("예외를 던지고 정산 건을 잠그거나 저장하지 않으며 모임을 읽지 않는다")
            void shouldThrowLinkTokenNotFoundException() {
                // given
                givenLinkTokenNotFound();
                MarkSharesSentCommand command = command();

                // when & then
                assertThatThrownBy(() -> shareCommandService.markSent(command))
                        .isInstanceOf(LinkTokenNotFoundException.class);
                then(settlementRepository).shouldHaveNoInteractions();
                then(gatheringRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("그 결제자에게 낼 분담금이 없는 경우")
        class WhenSharesNotFound {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowSettlementShareNotFoundException() {
                // given
                givenParticipant(LEE_ID);
                givenSharedSettlements();
                MarkSharesSentCommand command = command();

                // when & then
                assertThatThrownBy(() -> shareCommandService.markSent(command))
                        .isInstanceOf(SettlementShareNotFoundException.class);
                thenSettlementNotSaved();
                then(gatheringRepository).shouldHaveNoInteractions();
                then(eventPublisher).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("그 결제자에게 낼 분담금이 모두 미입금인 경우")
        class WhenSharesUnpaid {

            @Test
            @DisplayName("요청한 참여자와 결제자로 정산 건을 잠가 읽고, 정산 건마다 분담금을 보냄으로 바꿔 저장해 반환한다")
            void shouldSaveSentShareForEachSettlementOfPayer() {
                // given
                givenParticipant(LEE_ID);
                givenSharedSettlements(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, ROUND_ID, PAYER_ID, PARTICIPANT_IDS),
                        SettlementFixture.SECOND_ROUND.createWithId(
                                THIRD_SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                List.of(PAYER_ID, LEE_ID)));
                givenGathering();
                givenSaveReturnsSettlement();

                // when
                PayerSharesResult result = shareCommandService.markSent(command());

                // then
                then(settlementRepository)
                        .should()
                        .findAllByShareParticipantIdAndPayerForUpdate(LEE_ID, PAYER_ID);
                ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
                then(settlementRepository).should(times(2)).save(captor.capture());
                assertThat(captor.getAllValues())
                        .extracting(Settlement::getId)
                        .containsExactly(SETTLEMENT_ID, THIRD_SETTLEMENT_ID);
                assertThat(result.payerParticipantId()).isEqualTo(PAYER_ID);
                assertThat(result.payerName()).isEqualTo(ParticipantFixture.KIM.getName());
                assertThat(result.unpaidAmount()).isZero();
                assertThat(result.hasUnpaidShares()).isFalse();
                assertThat(result.shares())
                        .extracting(
                                ShareBreakdownResult::settlementId,
                                ShareBreakdownResult::amount,
                                ShareBreakdownResult::status)
                        .containsExactly(
                                tuple(SETTLEMENT_ID, 3_333L, ShareStatus.SENT),
                                tuple(THIRD_SETTLEMENT_ID, 10_000L, ShareStatus.SENT));
                assertThat(captor.getAllValues())
                        .extracting(
                                settlement -> settlement.getShareOf(LEE_ID).getStatusChangedAt())
                        .containsOnly(NOW);
                then(eventPublisher).should().publishEvent(new SharesSentEvent());
            }
        }

        @Nested
        @DisplayName("같은 차수에 다른 결제자의 정산 건이 있는 경우")
        class WhenRoundHasOtherSettlements {

            @Test
            @DisplayName("본인이 인원이 아닌 정산 건까지 합쳐 그 차수 참여자의 이름을 참여한 순서대로 담아 반환한다")
            void shouldReturnUnionOfRoundParticipantNames() {
                // given
                Settlement mySettlement =
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                List.of(PAYER_ID, LEE_ID));
                Settlement otherSettlement =
                        SettlementFixture.SECOND_ROUND.createWithId(
                                SECOND_SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PARK_ID,
                                List.of(PARK_ID, PAYER_ID));
                givenParticipant(LEE_ID);
                givenSharedSettlements(mySettlement);
                givenGatheringSettlements(otherSettlement, mySettlement);
                givenGathering();
                givenSaveReturnsSettlement();

                // when
                PayerSharesResult result = shareCommandService.markSent(command());

                // then
                assertThat(result.shares())
                        .singleElement()
                        .extracting(ShareBreakdownResult::roundParticipantNames)
                        .isEqualTo(
                                List.of(
                                        ParticipantFixture.KIM.getName(),
                                        ParticipantFixture.LEE.getName(),
                                        ParticipantFixture.PARK.getName()));
            }

            @Test
            @DisplayName("읽는 사이에 모임의 정산 건에서 본인 정산 건이 빠져도 본인 정산 건 인원의 이름은 담는다")
            void shouldIncludeOwnSettlementParticipantsWhenMissingFromGatheringSettlements() {
                // given
                Settlement mySettlement =
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                List.of(PAYER_ID, LEE_ID));
                givenParticipant(LEE_ID);
                givenSharedSettlements(mySettlement);
                givenGatheringSettlements();
                givenGathering();
                givenSaveReturnsSettlement();

                // when
                PayerSharesResult result = shareCommandService.markSent(command());

                // then
                assertThat(result.shares())
                        .singleElement()
                        .extracting(ShareBreakdownResult::roundParticipantNames)
                        .isEqualTo(
                                List.of(
                                        ParticipantFixture.KIM.getName(),
                                        ParticipantFixture.LEE.getName()));
            }

            @Test
            @DisplayName("취소한 정산 건에만 있는 참여자의 이름은 담지 않는다")
            void shouldExcludeParticipantsOfCancelledSettlements() {
                // given
                Settlement mySettlement =
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                List.of(PAYER_ID, LEE_ID));
                Settlement cancelledSettlement =
                        SettlementFixture.SECOND_ROUND.createCancelled(
                                SECOND_SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PARK_ID,
                                List.of(PARK_ID, PAYER_ID));
                givenParticipant(LEE_ID);
                givenSharedSettlements(mySettlement);
                givenGatheringSettlements(cancelledSettlement, mySettlement);
                givenGathering();
                givenSaveReturnsSettlement();

                // when
                PayerSharesResult result = shareCommandService.markSent(command());

                // then
                assertThat(result.shares())
                        .singleElement()
                        .extracting(
                                ShareBreakdownResult::roundId,
                                ShareBreakdownResult::roundNumber,
                                ShareBreakdownResult::roundParticipantNames)
                        .containsExactly(
                                ROUND_ID,
                                GatheringRoundFixture.FIRST.getNumber(),
                                List.of(
                                        ParticipantFixture.KIM.getName(),
                                        ParticipantFixture.LEE.getName()));
            }
        }

        @Nested
        @DisplayName("이미 보냄이나 확인 완료인 분담금이 있는 경우")
        class WhenSharesAlreadySent {

            @Test
            @DisplayName("그 정산 건은 저장하지 않고 미입금인 정산 건만 보냄으로 바꿔 저장한다")
            void shouldSaveOnlyUnpaidSettlement() {
                // given
                givenParticipant(LEE_ID);
                givenSharedSettlements(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.SENT),
                        SettlementFixture.SECOND_ROUND.createWithFirstShareStatus(
                                SECOND_SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.CONFIRMED),
                        SettlementFixture.DINNER.createWithId(
                                THIRD_SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS));
                givenGathering();
                givenSaveReturnsSettlement();

                // when
                PayerSharesResult result = shareCommandService.markSent(command());

                // then
                ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
                then(settlementRepository).should().save(captor.capture());
                assertThat(captor.getValue().getId()).isEqualTo(THIRD_SETTLEMENT_ID);
                assertThat(result.shares())
                        .extracting(
                                ShareBreakdownResult::settlementId, ShareBreakdownResult::status)
                        .containsExactly(
                                tuple(SETTLEMENT_ID, ShareStatus.SENT),
                                tuple(SECOND_SETTLEMENT_ID, ShareStatus.CONFIRMED),
                                tuple(THIRD_SETTLEMENT_ID, ShareStatus.SENT));
            }

            @Test
            @DisplayName("미입금 분담금이 없으면 아무것도 저장하지 않고 예외 없이 반환한다")
            void shouldReturnWithoutSavingWhenUnpaidSharesNotExist() {
                // given
                givenParticipant(LEE_ID);
                givenSharedSettlements(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.SENT),
                        SettlementFixture.SECOND_ROUND.createWithFirstShareStatus(
                                SECOND_SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.CONFIRMED));
                givenGathering();

                // when
                PayerSharesResult result = shareCommandService.markSent(command());

                // then
                thenSettlementNotSaved();
                then(eventPublisher).shouldHaveNoInteractions();
                assertThat(result.hasUnpaidShares()).isFalse();
                assertThat(result.unpaidAmount()).isZero();
                assertThat(result.shares())
                        .extracting(ShareBreakdownResult::status)
                        .containsExactly(ShareStatus.SENT, ShareStatus.CONFIRMED);
            }
        }

        private MarkSharesSentCommand command() {
            return new MarkSharesSentCommand(LINK_TOKEN, PAYER_ID);
        }

        // 모임의 정산 건에는 LEE가 분담금을 내지 않는 정산 건과 취소한 정산 건도 들어 있을 수 있다.
        private void givenGatheringSettlements(Settlement... settlements) {
            given(settlementRepository.findAllByGatheringId(GATHERING_ID))
                    .willReturn(List.of(settlements));
        }

        private void givenSharedSettlements(Settlement... settlements) {
            given(
                            settlementRepository.findAllByShareParticipantIdAndPayerForUpdate(
                                    LEE_ID, PAYER_ID))
                    .willReturn(List.of(settlements));
        }

        private void givenGathering() {
            given(gatheringRepository.findByParticipantId(LEE_ID))
                    .willReturn(Optional.of(gathering()));
        }
    }

    @Nested
    @DisplayName("confirm 메서드는")
    class Confirm {

        // 토큰이 없거나 모임의 파기 시각이 지났거나 데이터 키를 지운 경우다. 갈래별 판단은 ParticipantQueryServiceTest가 검증한다.
        @Nested
        @DisplayName("개인 링크 토큰으로 참여자를 찾지 못한 경우")
        class WhenLinkTokenNotFound {

            @Test
            @DisplayName("예외를 던지고 정산 건을 읽지 않는다")
            void shouldThrowLinkTokenNotFoundException() {
                // given
                givenLinkTokenNotFound();
                ConfirmShareCommand command = command(LEE_ID);

                // when & then
                assertThatThrownBy(() -> shareCommandService.confirm(command))
                        .isInstanceOf(LinkTokenNotFoundException.class);
                then(settlementRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("요청한 참여자가 결제자가 아닌 경우")
        class WhenParticipantIsNotPayer {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowNotSettlementPayerException() {
                // given
                givenParticipant(LEE_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, ROUND_ID, PAYER_ID, PARTICIPANT_IDS));
                ConfirmShareCommand command = command(LEE_ID);

                // when & then
                assertThatThrownBy(() -> shareCommandService.confirm(command))
                        .isInstanceOf(NotSettlementPayerException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("이미 확인 완료인 분담금인 경우")
        class WhenShareConfirmed {

            @Test
            @DisplayName("같은 요청을 다시 보낸 것으로 보고 정산 건을 저장하지 않고 확인 완료인 현재 상태를 반환한다")
            void shouldReturnCurrentSettlementWithoutSaving() {
                // given
                givenParticipant(PAYER_ID);
                Settlement settlement =
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, ROUND_ID, PAYER_ID, PARTICIPANT_IDS);
                settlement.confirmShare(LEE_ID, STATUS_CHANGED_AT);
                givenSettlement(settlement);
                givenGatheringOfPayer();
                ConfirmShareCommand command = command(LEE_ID);

                // when
                SettlementResult result = shareCommandService.confirm(command);

                // then
                assertThat(result.shares())
                        .extracting(
                                SettlementShareResult::participantId, SettlementShareResult::status)
                        .contains(tuple(LEE_ID, ShareStatus.CONFIRMED));
                assertThat(settlement.getShareOf(LEE_ID).getStatusChangedAt())
                        .isEqualTo(STATUS_CHANGED_AT);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("확인 완료로 시작한 0원 분담금인 경우")
        class WhenShareAmountZero {

            @Test
            @DisplayName("이미 확인 완료가 아니라 0원 분담금이라는 예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowZeroShareStatusUnchangeableException() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(settlementWithZeroShare());
                ConfirmShareCommand command = command(PARK_ID);

                // when & then
                assertThatThrownBy(() -> shareCommandService.confirm(command))
                        .isInstanceOf(ZeroShareStatusUnchangeableException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("정산 건에 분담금이 없는 참여자인 경우")
        class WhenShareNotFound {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowSettlementShareNotFoundException() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, ROUND_ID, PAYER_ID, PARTICIPANT_IDS));
                ConfirmShareCommand command = command(OUTSIDER_ID);

                // when & then
                assertThatThrownBy(() -> shareCommandService.confirm(command))
                        .isInstanceOf(SettlementShareNotFoundException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("취소한 정산 건인 경우")
        class WhenSettlementCancelled {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowSettlementAlreadyCancelledException() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createCancelled(
                                SETTLEMENT_ID, GATHERING_ID, ROUND_ID, PAYER_ID, PARTICIPANT_IDS));
                ConfirmShareCommand command = command(LEE_ID);

                // when & then
                assertThatThrownBy(() -> shareCommandService.confirm(command))
                        .isInstanceOf(SettlementAlreadyCancelledException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("확인 완료로 바꿀 수 있는 경우")
        class WhenConfirmable {

            @Test
            @DisplayName("보냄 분담금을 확인 완료로 바꿔 저장하고 확인한 금액을 담아 반환한다")
            void shouldConfirmSentShare() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.SENT));
                givenGatheringOfPayer();
                givenSaveReturnsSettlement();

                // when
                SettlementResult result = shareCommandService.confirm(command(LEE_ID));

                // then
                then(settlementRepository).should().save(any(Settlement.class));
                assertThat(result.shares())
                        .extracting(
                                SettlementShareResult::participantId, SettlementShareResult::status)
                        .containsExactly(
                                tuple(LEE_ID, ShareStatus.CONFIRMED),
                                tuple(PARK_ID, ShareStatus.UNPAID));
                assertThat(result.receivableAmount()).isEqualTo(6_666L);
                assertThat(result.confirmedAmount()).isEqualTo(3_333L);
                assertThat(result.sentAmount()).isZero();
            }

            @Test
            @DisplayName("미입금 분담금도 확인 완료로 바꾸고, 보냄 분담금은 따로 더해 반환한다")
            void shouldConfirmUnpaidShare() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.SENT));
                givenGatheringOfPayer();
                givenSaveReturnsSettlement();

                // when
                SettlementResult result = shareCommandService.confirm(command(PARK_ID));

                // then
                assertThat(result.shares())
                        .extracting(
                                SettlementShareResult::participantId, SettlementShareResult::status)
                        .containsExactly(
                                tuple(LEE_ID, ShareStatus.SENT),
                                tuple(PARK_ID, ShareStatus.CONFIRMED));
                assertThat(result.confirmedAmount()).isEqualTo(3_333L);
                assertThat(result.sentAmount()).isEqualTo(3_333L);
            }

            @Test
            @DisplayName("정산 건이 속한 차수의 id와 번호를 담아 반환한다")
            void shouldReturnRoundOfSettlement() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                SECOND_ROUND_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.SENT));
                givenGatheringOfPayer();
                givenSaveReturnsSettlement();

                // when
                SettlementResult result = shareCommandService.confirm(command(LEE_ID));

                // then
                assertThat(result.roundId()).isEqualTo(SECOND_ROUND_ID);
                assertThat(result.roundNumber())
                        .isEqualTo(GatheringRoundFixture.SECOND.getNumber());
            }
        }

        private ConfirmShareCommand command(Long participantId) {
            return ConfirmShareCommand.of(LINK_TOKEN, SETTLEMENT_ID, participantId);
        }
    }

    @Nested
    @DisplayName("revert 메서드는")
    class Revert {

        // 토큰이 없거나 모임의 파기 시각이 지났거나 데이터 키를 지운 경우다. 갈래별 판단은 ParticipantQueryServiceTest가 검증한다.
        @Nested
        @DisplayName("개인 링크 토큰으로 참여자를 찾지 못한 경우")
        class WhenLinkTokenNotFound {

            @Test
            @DisplayName("예외를 던지고 정산 건을 읽지 않는다")
            void shouldThrowLinkTokenNotFoundException() {
                // given
                givenLinkTokenNotFound();
                RevertShareCommand command = command();

                // when & then
                assertThatThrownBy(() -> shareCommandService.revert(command))
                        .isInstanceOf(LinkTokenNotFoundException.class);
                then(settlementRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("요청한 참여자가 결제자가 아닌 경우")
        class WhenParticipantIsNotPayer {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowNotSettlementPayerException() {
                // given
                givenParticipant(LEE_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.SENT));
                RevertShareCommand command = command();

                // when & then
                assertThatThrownBy(() -> shareCommandService.revert(command))
                        .isInstanceOf(NotSettlementPayerException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("이미 미입금인 분담금인 경우")
        class WhenShareUnpaid {

            @Test
            @DisplayName("같은 요청을 다시 보낸 것으로 보고 정산 건을 저장하지 않고 미입금인 현재 상태를 반환한다")
            void shouldReturnCurrentSettlementWithoutSaving() {
                // given
                givenParticipant(PAYER_ID);
                Settlement settlement =
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, ROUND_ID, PAYER_ID, PARTICIPANT_IDS);
                settlement.confirmShare(LEE_ID, STATUS_CHANGED_AT.minusMinutes(1));
                settlement.revertShareToUnpaid(LEE_ID, STATUS_CHANGED_AT);
                givenSettlement(settlement);
                givenGatheringOfPayer();
                RevertShareCommand command = command();

                // when
                SettlementResult result = shareCommandService.revert(command);

                // then
                assertThat(result.shares())
                        .extracting(
                                SettlementShareResult::participantId, SettlementShareResult::status)
                        .contains(tuple(LEE_ID, ShareStatus.UNPAID));
                assertThat(settlement.getShareOf(LEE_ID).getStatusChangedAt())
                        .isEqualTo(STATUS_CHANGED_AT);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("확인 완료로 시작한 0원 분담금인 경우")
        class WhenShareAmountZero {

            @Test
            @DisplayName("예외를 던지고 정산 건을 저장하지 않는다")
            void shouldThrowZeroShareStatusUnchangeableException() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(settlementWithZeroShare());
                RevertShareCommand command =
                        RevertShareCommand.of(LINK_TOKEN, SETTLEMENT_ID, PARK_ID);

                // when & then
                assertThatThrownBy(() -> shareCommandService.revert(command))
                        .isInstanceOf(ZeroShareStatusUnchangeableException.class);
                thenSettlementNotSaved();
            }
        }

        @Nested
        @DisplayName("미입금으로 되돌릴 수 있는 경우")
        class WhenRevertible {

            @Test
            @DisplayName("보냄 분담금을 미입금으로 되돌려 저장하고 반환한다")
            void shouldRevertSentShare() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.SENT));
                givenGatheringOfPayer();
                givenSaveReturnsSettlement();

                // when
                SettlementResult result = shareCommandService.revert(command());

                // then
                then(settlementRepository).should().save(any(Settlement.class));
                assertThat(result.shares())
                        .extracting(SettlementShareResult::status)
                        .containsOnly(ShareStatus.UNPAID);
                assertThat(result.sentAmount()).isZero();
            }

            @Test
            @DisplayName("확인 완료 분담금도 미입금으로 되돌려 저장하고 반환한다")
            void shouldRevertConfirmedShare() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                ROUND_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.CONFIRMED));
                givenGatheringOfPayer();
                givenSaveReturnsSettlement();

                // when
                SettlementResult result = shareCommandService.revert(command());

                // then
                then(settlementRepository).should().save(any(Settlement.class));
                assertThat(result.shares())
                        .extracting(SettlementShareResult::status)
                        .containsOnly(ShareStatus.UNPAID);
                assertThat(result.confirmedAmount()).isZero();
            }

            @Test
            @DisplayName("정산 건이 속한 차수의 id와 번호를 담아 반환한다")
            void shouldReturnRoundOfSettlement() {
                // given
                givenParticipant(PAYER_ID);
                givenSettlement(
                        SettlementFixture.DINNER.createWithFirstShareStatus(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                SECOND_ROUND_ID,
                                PAYER_ID,
                                PARTICIPANT_IDS,
                                ShareStatus.SENT));
                givenGatheringOfPayer();
                givenSaveReturnsSettlement();

                // when
                SettlementResult result = shareCommandService.revert(command());

                // then
                assertThat(result.roundId()).isEqualTo(SECOND_ROUND_ID);
                assertThat(result.roundNumber())
                        .isEqualTo(GatheringRoundFixture.SECOND.getNumber());
            }
        }

        private RevertShareCommand command() {
            return RevertShareCommand.of(LINK_TOKEN, SETTLEMENT_ID, LEE_ID);
        }
    }

    private Gathering gathering() {
        return GatheringFixture.FRIDAY_DINNER.createWithRounds(
                GATHERING_ID,
                OPENED_AT,
                List.of(
                        ParticipantFixture.KIM.createWithId(PAYER_ID),
                        ParticipantFixture.LEE.createWithId(LEE_ID),
                        ParticipantFixture.PARK.createWithId(PARK_ID)),
                List.of(
                        GatheringRoundFixture.FIRST.create(),
                        GatheringRoundFixture.SECOND.create()));
    }

    // confirm과 revert는 응답에 차수 번호를 담으려고 정산 건이 속한 모임을 읽는다.
    private void givenGatheringOfPayer() {
        given(gatheringRepository.findById(GATHERING_ID)).willReturn(Optional.of(gathering()));
    }

    private void givenParticipant(Long participantId) {
        given(getLinkedParticipantUseCase.getLinkedParticipant(LINK_TOKEN))
                .willReturn(new LinkedParticipantResult(participantId, DELETES_AT));
    }

    private void givenLinkTokenNotFound() {
        given(getLinkedParticipantUseCase.getLinkedParticipant(LINK_TOKEN))
                .willThrow(new LinkTokenNotFoundException());
    }

    private void givenSettlement(Settlement settlement) {
        given(settlementRepository.findById(SETTLEMENT_ID)).willReturn(Optional.of(settlement));
    }

    private void givenSaveReturnsSettlement() {
        given(settlementRepository.save(any(Settlement.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenNow(LocalDateTime now) {
        Clock clock =
                Clock.fixed(
                        now.atZone(TimeConfig.SERVICE_ZONE).toInstant(), TimeConfig.SERVICE_ZONE);
        shareCommandService =
                new ShareCommandService(
                        getLinkedParticipantUseCase,
                        gatheringRepository,
                        settlementRepository,
                        eventPublisher,
                        clock);
    }

    private void thenSettlementNotSaved() {
        then(settlementRepository).should(never()).save(any(Settlement.class));
    }

    // 박의 분담금을 0원으로 지정해 확인 완료로 시작하고, 이의 분담금은 미입금인 정산 건이다.
    private Settlement settlementWithZeroShare() {
        return SettlementFixture.DINNER.createWithFixedAmounts(
                SETTLEMENT_ID,
                GATHERING_ID,
                ROUND_ID,
                PAYER_ID,
                PARTICIPANT_IDS,
                Map.of(PARK_ID, 0L));
    }
}
