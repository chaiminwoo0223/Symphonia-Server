package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;

import com.symphonia.UnitTest;
import com.symphonia.gathering.application.dto.result.SettlementFormResult;
import com.symphonia.gathering.application.dto.result.SettlementParticipantResult;
import com.symphonia.gathering.application.dto.result.SettlementResult;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.exception.ParticipantNotFoundException;
import com.symphonia.gathering.domain.policy.SettlementPolicy;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.ParticipantBankAccountRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.fixture.GatheringFixture;
import com.symphonia.gathering.fixture.ParticipantFixture;
import com.symphonia.gathering.fixture.SettlementFixture;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("SettlementQueryService 단위 테스트")
class SettlementQueryServiceTest extends UnitTest {

    private static final Long GATHERING_ID = 10L;
    private static final Long SETTLEMENT_ID = 30L;
    private static final Long PAYER_ID = 20L;
    private static final Long LEE_ID = 21L;
    private static final Long PARK_ID = 22L;
    private static final String LINK_TOKEN = "link-token";

    @InjectMocks private SettlementQueryService settlementQueryService;

    @Mock private ParticipantLinkTokenRepository participantLinkTokenRepository;
    @Mock private GatheringRepository gatheringRepository;
    @Mock private ParticipantBankAccountRepository participantBankAccountRepository;
    @Mock private SettlementRepository settlementRepository;

    @Nested
    @DisplayName("getForm 메서드는")
    class GetForm {

        @Nested
        @DisplayName("개인 링크 토큰에 해당하는 참여자가 없는 경우")
        class WhenLinkTokenNotFound {

            @Test
            @DisplayName("예외를 던진다")
            void shouldThrowParticipantNotFoundException() {
                // given
                given(participantLinkTokenRepository.findParticipantIdByValue(LINK_TOKEN))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> settlementQueryService.getForm(LINK_TOKEN))
                        .isInstanceOf(ParticipantNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("모임의 첫 정산 건을 기록하는 경우")
        class WhenFirstSettlement {

            @Test
            @DisplayName("참여자 전원을 인원 기본값으로 하고 기록 가능, 계좌 미등록, 현재 계좌 동의 버전을 반환한다")
            void shouldReturnAllParticipantsAsDefault() {
                // given
                givenForm(LocalDateTime.now().minusHours(1), Optional.empty(), false, List.of());

                // when
                SettlementFormResult result = settlementQueryService.getForm(LINK_TOKEN);

                // then
                assertThat(result.recordable()).isTrue();
                assertThat(result.bankAccountRegistered()).isFalse();
                assertThat(result.bankAccountConsentVersion())
                        .isEqualTo(SettlementPolicy.BANK_ACCOUNT_CONSENT_VERSION);
                assertThat(result.participants())
                        .extracting(
                                SettlementParticipantResult::id, SettlementParticipantResult::name)
                        .containsExactly(
                                tuple(PAYER_ID, ParticipantFixture.KIM.getName()),
                                tuple(LEE_ID, ParticipantFixture.LEE.getName()),
                                tuple(PARK_ID, ParticipantFixture.PARK.getName()));
                assertThat(result.defaultParticipantIds())
                        .containsExactly(PAYER_ID, LEE_ID, PARK_ID);
                assertThat(result.settlements()).isEmpty();
            }
        }

        @Nested
        @DisplayName("직전 정산 건이 있는 경우")
        class WhenLatestSettlementExists {

            @Test
            @DisplayName("직전 정산 건의 인원을 결제자부터 인원 기본값으로 반환한다")
            void shouldReturnLatestSettlementParticipantsAsDefault() {
                // given
                Settlement latest =
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID, GATHERING_ID, LEE_ID, List.of(PAYER_ID, LEE_ID));
                givenForm(LocalDateTime.now().minusHours(1), Optional.of(latest), false, List.of());

                // when
                SettlementFormResult result = settlementQueryService.getForm(LINK_TOKEN);

                // then
                assertThat(result.defaultParticipantIds()).containsExactly(LEE_ID, PAYER_ID);
            }
        }

        @Nested
        @DisplayName("요청한 참여자가 결제자로 기록한 정산 건이 있는 경우")
        class WhenPayerSettlementsExist {

            @Test
            @DisplayName("계좌 등록 여부와 함께 그 정산 건을 분담과 함께 반환한다")
            void shouldReturnPayerSettlements() {
                // given
                Settlement settlement =
                        SettlementFixture.DINNER.createWithId(
                                SETTLEMENT_ID,
                                GATHERING_ID,
                                PAYER_ID,
                                List.of(PAYER_ID, LEE_ID, PARK_ID));
                givenForm(
                        LocalDateTime.now().minusHours(1),
                        Optional.of(settlement),
                        true,
                        List.of(settlement));

                // when
                SettlementFormResult result = settlementQueryService.getForm(LINK_TOKEN);

                // then
                assertThat(result.bankAccountRegistered()).isTrue();
                assertThat(result.settlements()).hasSize(1);
                SettlementResult settlementResult = result.settlements().getFirst();
                assertThat(settlementResult.id()).isEqualTo(SETTLEMENT_ID);
                assertThat(settlementResult.payerParticipantId()).isEqualTo(PAYER_ID);
                assertThat(settlementResult.payerAmount()).isEqualTo(3_334L);
                assertThat(settlementResult.shares()).hasSize(2);
            }
        }

        @Nested
        @DisplayName("모임을 만든 지 48시간이 지난 경우")
        class WhenRecordingPeriodPassed {

            @Test
            @DisplayName("기록 불가로 표시한다")
            void shouldReturnNotRecordableForm() {
                // given
                givenForm(LocalDateTime.now().minusHours(49), Optional.empty(), true, List.of());

                // when
                SettlementFormResult result = settlementQueryService.getForm(LINK_TOKEN);

                // then
                assertThat(result.recordable()).isFalse();
            }
        }

        private void givenForm(
                LocalDateTime createdAt,
                Optional<Settlement> latestSettlement,
                boolean bankAccountRegistered,
                List<Settlement> payerSettlements) {
            given(participantLinkTokenRepository.findParticipantIdByValue(LINK_TOKEN))
                    .willReturn(Optional.of(PAYER_ID));
            given(gatheringRepository.findByParticipantId(PAYER_ID))
                    .willReturn(
                            Optional.of(
                                    GatheringFixture.FRIDAY_DINNER.createWithIdAndCreatedAt(
                                            GATHERING_ID,
                                            createdAt,
                                            List.of(
                                                    ParticipantFixture.KIM.createWithId(PAYER_ID),
                                                    ParticipantFixture.LEE.createWithId(LEE_ID),
                                                    ParticipantFixture.PARK.createWithId(
                                                            PARK_ID)))));
            given(settlementRepository.findLatestByGatheringId(GATHERING_ID))
                    .willReturn(latestSettlement);
            given(participantBankAccountRepository.existsByParticipantId(PAYER_ID))
                    .willReturn(bankAccountRegistered);
            given(settlementRepository.findAllByPayerParticipantId(PAYER_ID))
                    .willReturn(payerSettlements);
        }
    }
}
