package com.symphonia.pairing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.catalog.application.dto.query.CatalogReferencesQuery;
import com.symphonia.catalog.application.dto.result.CatalogReferencesResult;
import com.symphonia.catalog.application.usecase.GetCatalogReferencesUseCase;
import com.symphonia.pairing.application.dto.query.GetMemberPairingFeedbacksQuery;
import com.symphonia.pairing.application.dto.result.MemberPairingFeedbackResult;
import com.symphonia.pairing.application.dto.result.MemberPairingFeedbacksResult;
import com.symphonia.pairing.domain.entity.PairingFeedback;
import com.symphonia.pairing.domain.repository.PairingFeedbackRepository;
import com.symphonia.pairing.fixture.PairingFeedbackFixture;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("PairingFeedbackQueryService 단위 테스트")
class PairingFeedbackQueryServiceTest extends UnitTest {

    private static final Long MEMBER_ID = 1L;
    private static final int SIZE = 2;

    @InjectMocks private PairingFeedbackQueryService pairingFeedbackQueryService;

    @Mock private PairingFeedbackRepository pairingFeedbackRepository;
    @Mock private GetCatalogReferencesUseCase getCatalogReferencesUseCase;

    @Nested
    @DisplayName("getFeedbacks 메서드는")
    class GetFeedbacks {

        @Nested
        @DisplayName("낸 피드백이 없는 경우")
        class WhenNoFeedbacks {

            @Test
            @DisplayName("빈 목록과 다음 커서 없음을 반환한다")
            void shouldReturnEmptyFeedbacks() {
                // given
                given(
                                pairingFeedbackRepository.findAllByMemberIdBeforeId(
                                        MEMBER_ID, null, SIZE + 1))
                        .willReturn(List.of());

                // when
                MemberPairingFeedbacksResult result =
                        pairingFeedbackQueryService.getFeedbacks(
                                GetMemberPairingFeedbacksQuery.of(MEMBER_ID, null, SIZE));

                // then
                assertThat(result.feedbacks()).isEmpty();
                assertThat(result.nextCursor()).isNull();
            }
        }

        @Nested
        @DisplayName("요청한 수보다 피드백이 더 남은 경우")
        class WhenMoreFeedbacksRemain {

            @Test
            @DisplayName("요청한 수만큼만 반환하고 마지막 피드백 id를 다음 커서로 반환한다")
            void shouldReturnPageAndNextCursor() {
                // given
                given(pairingFeedbackRepository.findAllByMemberIdBeforeId(MEMBER_ID, 50L, SIZE + 1))
                        .willReturn(
                                List.of(feedback(30L, 1L), feedback(20L, 2L), feedback(10L, 3L)));
                givenReferencesOf(1L, 2L);

                // when
                MemberPairingFeedbacksResult result =
                        pairingFeedbackQueryService.getFeedbacks(
                                GetMemberPairingFeedbacksQuery.of(MEMBER_ID, 50L, SIZE));

                // then
                assertThat(result.feedbacks())
                        .extracting(MemberPairingFeedbackResult::drinkId)
                        .containsExactly(1L, 2L);
                assertThat(result.nextCursor()).isEqualTo(20L);
            }
        }

        @Nested
        @DisplayName("마지막 페이지인 경우")
        class WhenLastPage {

            @Test
            @DisplayName("읽은 피드백을 모두 반환하고 다음 커서 없음을 반환한다")
            void shouldReturnRemainingFeedbacksWithoutNextCursor() {
                // given
                given(pairingFeedbackRepository.findAllByMemberIdBeforeId(MEMBER_ID, 20L, SIZE + 1))
                        .willReturn(List.of(feedback(10L, 3L)));
                givenReferencesOf(3L);

                // when
                MemberPairingFeedbacksResult result =
                        pairingFeedbackQueryService.getFeedbacks(
                                GetMemberPairingFeedbacksQuery.of(MEMBER_ID, 20L, SIZE));

                // then
                assertThat(result.feedbacks()).hasSize(1);
                assertThat(result.nextCursor()).isNull();
            }
        }

        @Nested
        @DisplayName("피드백이 있는 경우")
        class WhenFeedbacksExist {

            @Test
            @DisplayName("술과 안주의 이름은 catalog에서 읽어 평점, 관계, 분위기와 함께 담는다")
            void shouldReturnNamesFromCatalog() {
                // given
                PairingFeedback feedback = feedback(10L, 3L);
                given(
                                pairingFeedbackRepository.findAllByMemberIdBeforeId(
                                        MEMBER_ID, null, SIZE + 1))
                        .willReturn(List.of(feedback));
                givenReferencesOf(3L);

                // when
                MemberPairingFeedbacksResult result =
                        pairingFeedbackQueryService.getFeedbacks(
                                GetMemberPairingFeedbacksQuery.of(MEMBER_ID, null, SIZE));

                // then
                assertThat(result.feedbacks())
                        .extracting(
                                MemberPairingFeedbackResult::drinkName,
                                MemberPairingFeedbackResult::anjuName,
                                MemberPairingFeedbackResult::rating,
                                MemberPairingFeedbackResult::relationshipType,
                                MemberPairingFeedbackResult::moodType)
                        .containsExactly(
                                tuple(
                                        "술3",
                                        "안주3",
                                        feedback.getRating(),
                                        feedback.getRelationshipType(),
                                        feedback.getMoodType()));
                then(getCatalogReferencesUseCase)
                        .should()
                        .getReferences(CatalogReferencesQuery.of(3L, 3L));
            }
        }
    }

    private PairingFeedback feedback(Long id, Long referenceId) {
        return PairingFeedbackFixture.FRIEND_FORMAL.createWithIdAndReferences(
                id, referenceId, referenceId);
    }

    private void givenReferencesOf(Long... referenceIds) {
        for (Long referenceId : referenceIds) {
            given(
                            getCatalogReferencesUseCase.getReferences(
                                    CatalogReferencesQuery.of(referenceId, referenceId)))
                    .willReturn(new CatalogReferencesResult("술" + referenceId, "안주" + referenceId));
        }
    }
}
