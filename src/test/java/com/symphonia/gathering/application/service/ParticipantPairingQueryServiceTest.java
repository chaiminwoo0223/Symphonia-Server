package com.symphonia.gathering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.catalog.application.dto.query.CatalogReferencesQuery;
import com.symphonia.catalog.application.dto.result.CatalogReferencesResult;
import com.symphonia.catalog.application.usecase.GetCatalogReferencesUseCase;
import com.symphonia.catalog.domain.exception.DrinkNotFoundException;
import com.symphonia.gathering.application.dto.result.LinkedParticipantResult;
import com.symphonia.gathering.application.dto.result.ParticipantPairingResult;
import com.symphonia.gathering.application.usecase.GetLinkedParticipantUseCase;
import com.symphonia.gathering.domain.exception.GatheringPairingNotFoundException;
import com.symphonia.gathering.domain.exception.LinkTokenNotFoundException;
import com.symphonia.gathering.domain.repository.GatheringPairingRepository;
import com.symphonia.gathering.domain.repository.ParticipantPairingFeedbackRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.ShareStatus;
import com.symphonia.gathering.fixture.GatheringPairingFixture;
import com.symphonia.gathering.fixture.SettlementFixture;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("ParticipantPairingQueryService 단위 테스트")
class ParticipantPairingQueryServiceTest extends UnitTest {

    private static final Long PARTICIPANT_ID = 20L;
    private static final Long OTHER_PARTICIPANT_ID = 21L;
    private static final Long GATHERING_ID = 10L;
    private static final Long ROUND_ID = 1L;
    private static final Long SETTLEMENT_ID = 40L;
    private static final String LINK_TOKEN = "link-token";
    private static final LocalDateTime DELETES_AT = LocalDateTime.of(2026, 11, 5, 18, 0);
    private static final GatheringPairingFixture PAIRING = GatheringPairingFixture.FRIEND_CASUAL;
    private static final CatalogReferencesResult CATALOG_REFERENCES =
            new CatalogReferencesResult("소주", "골뱅이무침");

    @InjectMocks private ParticipantPairingQueryService participantPairingQueryService;

    @Mock private GetLinkedParticipantUseCase getLinkedParticipantUseCase;
    @Mock private GatheringPairingRepository gatheringPairingRepository;
    @Mock private ParticipantPairingFeedbackRepository participantPairingFeedbackRepository;
    @Mock private SettlementRepository settlementRepository;
    @Mock private GetCatalogReferencesUseCase getCatalogReferencesUseCase;

    @Nested
    @DisplayName("getPairing 메서드는")
    class GetPairing {

        // 토큰이 없거나 모임의 파기 시각이 지났거나 데이터 키를 지운 경우다. 갈래별 판단은 ParticipantQueryServiceTest가 검증한다.
        @Nested
        @DisplayName("개인 링크 토큰으로 참여자를 찾지 못한 경우")
        class WhenLinkTokenNotFound {

            @Test
            @DisplayName("예외를 던지고 모임 페어링을 읽지 않는다")
            void shouldThrowLinkTokenNotFoundException() {
                // given
                given(getLinkedParticipantUseCase.getLinkedParticipant(LINK_TOKEN))
                        .willThrow(new LinkTokenNotFoundException());

                // when & then
                assertThatThrownBy(() -> participantPairingQueryService.getPairing(LINK_TOKEN))
                        .isInstanceOf(LinkTokenNotFoundException.class);
                then(gatheringPairingRepository).shouldHaveNoInteractions();
                then(getCatalogReferencesUseCase).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모임에서 고른 페어링이 없는 경우")
        class WhenGatheringPairingNotFound {

            @Test
            @DisplayName("예외를 던지고 카탈로그를 조회하지 않는다")
            void shouldThrowGatheringPairingNotFoundException() {
                // given
                givenLinkedParticipant();
                given(gatheringPairingRepository.findByParticipantId(PARTICIPANT_ID))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> participantPairingQueryService.getPairing(LINK_TOKEN))
                        .isInstanceOf(GatheringPairingNotFoundException.class);
                then(getCatalogReferencesUseCase).shouldHaveNoInteractions();
                then(participantPairingFeedbackRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("페어링이 가리키는 카탈로그가 사라진 경우")
        class WhenCatalogReferenceNotFound {

            @Test
            @DisplayName("카탈로그 조회 예외를 그대로 던진다")
            void shouldThrowDrinkNotFoundException() {
                // given
                givenGatheringPairing();
                given(getCatalogReferencesUseCase.getReferences(catalogReferencesQuery()))
                        .willThrow(new DrinkNotFoundException());

                // when & then
                assertThatThrownBy(() -> participantPairingQueryService.getPairing(LINK_TOKEN))
                        .isInstanceOf(DrinkNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("피드백을 내지 않았고 돈이 오가지 않은 참여자인 경우")
        class WhenFeedbackNotSubmittedAndMoneyNotMoved {

            @Test
            @DisplayName("모임 페어링의 카탈로그 이름과 함께 피드백을 내지 않았고 낼 수도 없다는 값을 반환한다")
            void shouldReturnResultNotSubmittable() {
                // given
                givenCatalogReferences();
                given(participantPairingFeedbackRepository.existsByParticipantId(PARTICIPANT_ID))
                        .willReturn(false);

                // when
                ParticipantPairingResult result =
                        participantPairingQueryService.getPairing(LINK_TOKEN);

                // then
                assertThat(result).isEqualTo(expectedResult(false, false));
            }
        }

        @Nested
        @DisplayName("피드백을 내지 않았고 본인 분담금을 보낸 참여자인 경우")
        class WhenFeedbackNotSubmittedAndShareSent {

            @Test
            @DisplayName("피드백을 내지 않았고 지금 낼 수 있다는 값을 반환한다")
            void shouldReturnResultSubmittable() {
                // given
                givenCatalogReferences();
                given(participantPairingFeedbackRepository.existsByParticipantId(PARTICIPANT_ID))
                        .willReturn(false);
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

                // when
                ParticipantPairingResult result =
                        participantPairingQueryService.getPairing(LINK_TOKEN);

                // then
                assertThat(result).isEqualTo(expectedResult(false, true));
            }
        }

        // 0원 분담금은 확인 완료로 시작하지만 돈이 오가지 않으므로 낼 수 없다.
        @Nested
        @DisplayName("피드백을 내지 않았고 본인 분담금이 0원인 참여자인 경우")
        class WhenFeedbackNotSubmittedAndShareAmountZero {

            @Test
            @DisplayName("피드백을 내지 않았고 낼 수 없다는 값을 반환한다")
            void shouldReturnResultNotSubmittable() {
                // given
                givenCatalogReferences();
                given(participantPairingFeedbackRepository.existsByParticipantId(PARTICIPANT_ID))
                        .willReturn(false);
                given(settlementRepository.findAllByShareParticipantId(PARTICIPANT_ID))
                        .willReturn(
                                List.of(
                                        SettlementFixture.DINNER.createWithFixedAmounts(
                                                SETTLEMENT_ID,
                                                GATHERING_ID,
                                                ROUND_ID,
                                                OTHER_PARTICIPANT_ID,
                                                List.of(OTHER_PARTICIPANT_ID, PARTICIPANT_ID),
                                                Map.of(PARTICIPANT_ID, 0L))));

                // when
                ParticipantPairingResult result =
                        participantPairingQueryService.getPairing(LINK_TOKEN);

                // then
                assertThat(result).isEqualTo(expectedResult(false, false));
            }
        }

        @Nested
        @DisplayName("이미 피드백을 낸 참여자인 경우")
        class WhenFeedbackAlreadySubmitted {

            @Test
            @DisplayName("모임 페어링의 카탈로그 이름과 피드백을 냈다는 값을 반환하고 정산 건을 읽지 않는다")
            void shouldReturnParticipantPairingResultSubmitted() {
                // given
                givenCatalogReferences();
                given(participantPairingFeedbackRepository.existsByParticipantId(PARTICIPANT_ID))
                        .willReturn(true);

                // when
                ParticipantPairingResult result =
                        participantPairingQueryService.getPairing(LINK_TOKEN);

                // then
                assertThat(result).isEqualTo(expectedResult(true, false));
                then(settlementRepository).shouldHaveNoInteractions();
            }
        }

        private ParticipantPairingResult expectedResult(
                boolean feedbackSubmitted, boolean feedbackSubmittable) {
            return new ParticipantPairingResult(
                    PAIRING.getDrinkId(),
                    CATALOG_REFERENCES.drinkName(),
                    PAIRING.getAnjuId(),
                    CATALOG_REFERENCES.anjuName(),
                    feedbackSubmitted,
                    feedbackSubmittable);
        }

        private CatalogReferencesQuery catalogReferencesQuery() {
            return CatalogReferencesQuery.of(PAIRING.getDrinkId(), PAIRING.getAnjuId());
        }

        private void givenCatalogReferences() {
            givenGatheringPairing();
            given(getCatalogReferencesUseCase.getReferences(catalogReferencesQuery()))
                    .willReturn(CATALOG_REFERENCES);
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
