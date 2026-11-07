package com.symphonia.pairing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.symphonia.UnitTest;
import com.symphonia.catalog.application.dto.query.CatalogReferencesQuery;
import com.symphonia.catalog.application.usecase.VerifyCatalogReferencesUseCase;
import com.symphonia.catalog.domain.exception.DrinkNotFoundException;
import com.symphonia.pairing.application.dto.command.SubmitPairingFeedbackCommand;
import com.symphonia.pairing.application.dto.result.PairingFeedbackResult;
import com.symphonia.pairing.domain.entity.PairingFeedback;
import com.symphonia.pairing.domain.repository.PairingFeedbackRepository;
import com.symphonia.pairing.domain.repository.PairingRatingSummaryRepository;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.PairingRating;
import com.symphonia.pairing.domain.vo.PairingRatingChange;
import com.symphonia.pairing.domain.vo.RelationshipType;
import com.symphonia.pairing.fixture.PairingFeedbackFixture;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("PairingCommandService 단위 테스트")
class PairingCommandServiceTest extends UnitTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long DRINK_ID = 1L;
    private static final Long ANJU_ID = 2L;
    private static final Long MUSIC_MOOD_ID = 3L;
    private static final Long FEEDBACK_ID = 4L;

    @InjectMocks private PairingCommandService pairingCommandService;

    @Mock private VerifyCatalogReferencesUseCase verifyCatalogReferencesUseCase;
    @Mock private PairingFeedbackRepository pairingFeedbackRepository;
    @Mock private PairingRatingSummaryRepository pairingRatingSummaryRepository;

    @Nested
    @DisplayName("submit 메서드는")
    class Submit {

        @Nested
        @DisplayName("참조하는 카탈로그가 존재하지 않는 경우")
        class WhenCatalogReferenceNotFound {

            @Test
            @DisplayName("카탈로그 검증 예외를 그대로 던지고 피드백을 저장하지 않는다")
            void shouldThrowDrinkNotFoundException() {
                // given
                willThrow(new DrinkNotFoundException())
                        .given(verifyCatalogReferencesUseCase)
                        .verify(CatalogReferencesQuery.of(DRINK_ID, ANJU_ID, MUSIC_MOOD_ID));

                // when & then
                assertThatThrownBy(() -> pairingCommandService.submit(command()))
                        .isInstanceOf(DrinkNotFoundException.class);
                then(pairingFeedbackRepository).shouldHaveNoInteractions();
                then(pairingRatingSummaryRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("회원이 같은 조합에 낸 피드백이 없는 경우")
        class WhenFeedbackNotSubmitted {

            @Test
            @DisplayName("피드백을 저장하고 조합별 집계에 평점을 더한 뒤 새로 만든 PairingFeedbackResult를 반환한다")
            void shouldSaveFeedbackAndAccumulateRating() {
                // given
                given(pairingFeedbackRepository.saveIfAbsent(any(PairingFeedback.class)))
                        .willReturn(true);
                givenLockedFeedback();

                // when
                PairingFeedbackResult result = pairingCommandService.submit(command());

                // then
                then(verifyCatalogReferencesUseCase)
                        .should()
                        .verify(CatalogReferencesQuery.of(DRINK_ID, ANJU_ID, MUSIC_MOOD_ID));
                ArgumentCaptor<PairingFeedback> inserted =
                        ArgumentCaptor.forClass(PairingFeedback.class);
                then(pairingFeedbackRepository).should().saveIfAbsent(inserted.capture());
                assertThat(inserted.getValue().getMemberId()).isEqualTo(MEMBER_ID);
                assertThat(inserted.getValue().getRating()).isEqualTo(PairingRating.LIKE);
                then(pairingFeedbackRepository).should(never()).save(any(PairingFeedback.class));
                then(pairingRatingSummaryRepository)
                        .should()
                        .accumulate(
                                DRINK_ID, ANJU_ID, MUSIC_MOOD_ID, new PairingRatingChange(1, 0));
                assertThat(result.id()).isEqualTo(FEEDBACK_ID);
                assertThat(result.memberId()).isEqualTo(MEMBER_ID);
                assertThat(result.drinkId()).isEqualTo(DRINK_ID);
                assertThat(result.anjuId()).isEqualTo(ANJU_ID);
                assertThat(result.musicMoodId()).isEqualTo(MUSIC_MOOD_ID);
                assertThat(result.rating()).isEqualTo(PairingRating.LIKE);
                assertThat(result.created()).isTrue();
            }
        }

        // 동시에 낸 첫 피드백 중 나중 요청도 saveIfAbsent가 false를 반환하므로 이 경우로 처리한다.
        @Nested
        @DisplayName("회원이 같은 조합에 낸 피드백이 있는 경우")
        class WhenFeedbackAlreadySubmitted {

            @Test
            @DisplayName("평점이 바뀌면 기존 피드백을 바꿔 저장하고 집계의 이전 평점을 빼고 새 평점을 더한다")
            void shouldUpdateFeedbackAndMoveRatingCountWhenRatingChanged() {
                // given
                givenSubmittedFeedback();
                SubmitPairingFeedbackCommand command =
                        command(RelationshipType.LOVER, MoodType.ROMANTIC, PairingRating.DISLIKE);

                // when
                PairingFeedbackResult result = pairingCommandService.submit(command);

                // then
                ArgumentCaptor<PairingFeedback> saved =
                        ArgumentCaptor.forClass(PairingFeedback.class);
                then(pairingFeedbackRepository).should().save(saved.capture());
                assertThat(saved.getValue().getId()).isEqualTo(FEEDBACK_ID);
                assertThat(saved.getValue().getRelationshipType())
                        .isEqualTo(RelationshipType.LOVER);
                assertThat(saved.getValue().getMoodType()).isEqualTo(MoodType.ROMANTIC);
                assertThat(saved.getValue().getRating()).isEqualTo(PairingRating.DISLIKE);
                then(pairingRatingSummaryRepository)
                        .should()
                        .accumulate(
                                DRINK_ID, ANJU_ID, MUSIC_MOOD_ID, new PairingRatingChange(-1, 1));
                assertThat(result.rating()).isEqualTo(PairingRating.DISLIKE);
                assertThat(result.created()).isFalse();
            }

            @Test
            @DisplayName("평점이 같으면 관계와 분위기만 바꿔 저장하고 집계는 바꾸지 않는다")
            void shouldUpdateFeedbackWithoutAccumulatingWhenRatingUnchanged() {
                // given
                givenSubmittedFeedback();
                SubmitPairingFeedbackCommand command =
                        command(RelationshipType.LOVER, MoodType.ROMANTIC, PairingRating.LIKE);

                // when
                PairingFeedbackResult result = pairingCommandService.submit(command);

                // then
                ArgumentCaptor<PairingFeedback> saved =
                        ArgumentCaptor.forClass(PairingFeedback.class);
                then(pairingFeedbackRepository).should().save(saved.capture());
                assertThat(saved.getValue().getRelationshipType())
                        .isEqualTo(RelationshipType.LOVER);
                assertThat(saved.getValue().getMoodType()).isEqualTo(MoodType.ROMANTIC);
                then(pairingRatingSummaryRepository).shouldHaveNoInteractions();
                assertThat(result.created()).isFalse();
            }

            // save는 받은 피드백을 그대로 돌려준다.
            private void givenSubmittedFeedback() {
                given(pairingFeedbackRepository.saveIfAbsent(any(PairingFeedback.class)))
                        .willReturn(false);
                givenLockedFeedback();
                given(pairingFeedbackRepository.save(any(PairingFeedback.class)))
                        .willAnswer(invocation -> invocation.getArgument(0));
            }
        }

        // FRIEND_FORMAL은 LIKE로 낸 피드백이다.
        private void givenLockedFeedback() {
            given(
                            pairingFeedbackRepository.findByMemberIdAndCombinationForUpdate(
                                    MEMBER_ID, DRINK_ID, ANJU_ID, MUSIC_MOOD_ID))
                    .willReturn(
                            Optional.of(
                                    PairingFeedbackFixture.FRIEND_FORMAL.createWithIdAndReferences(
                                            FEEDBACK_ID, DRINK_ID, ANJU_ID, MUSIC_MOOD_ID)));
        }

        private SubmitPairingFeedbackCommand command() {
            return command(RelationshipType.FRIEND, MoodType.FORMAL, PairingRating.LIKE);
        }

        private SubmitPairingFeedbackCommand command(
                RelationshipType relationshipType, MoodType moodType, PairingRating rating) {
            return new SubmitPairingFeedbackCommand(
                    MEMBER_ID,
                    DRINK_ID,
                    ANJU_ID,
                    MUSIC_MOOD_ID,
                    relationshipType,
                    moodType,
                    rating);
        }
    }
}
