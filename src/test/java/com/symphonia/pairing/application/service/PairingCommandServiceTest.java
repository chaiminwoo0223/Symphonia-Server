package com.symphonia.pairing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.symphonia.UnitTest;
import com.symphonia.catalog.application.dto.query.VerifyCatalogReferencesQuery;
import com.symphonia.catalog.application.usecase.VerifyCatalogReferencesUseCase;
import com.symphonia.catalog.domain.exception.DrinkNotFoundException;
import com.symphonia.pairing.application.dto.command.SubmitPairingFeedbackCommand;
import com.symphonia.pairing.application.dto.result.PairingFeedbackResult;
import com.symphonia.pairing.domain.entity.PairingFeedback;
import com.symphonia.pairing.domain.repository.PairingFeedbackRepository;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.PairingRating;
import com.symphonia.pairing.domain.vo.RelationshipType;
import com.symphonia.pairing.fixture.PairingFeedbackFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("PairingCommandService 단위 테스트")
class PairingCommandServiceTest extends UnitTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long DRINK_ID = 1L;
    private static final Long ANJU_ID = 1L;
    private static final Long MUSIC_MOOD_ID = 1L;

    @InjectMocks private PairingCommandService pairingCommandService;

    @Mock private VerifyCatalogReferencesUseCase verifyCatalogReferencesUseCase;
    @Mock private PairingFeedbackRepository pairingFeedbackRepository;

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
                        .verify(VerifyCatalogReferencesQuery.of(DRINK_ID, ANJU_ID, MUSIC_MOOD_ID));

                // when & then
                assertThatThrownBy(() -> pairingCommandService.submit(command()))
                        .isInstanceOf(DrinkNotFoundException.class);
                then(pairingFeedbackRepository).shouldHaveNoInteractions();
            }
        }

        @Nested
        @DisplayName("모두 존재하는 경우")
        class WhenAllFound {

            @Test
            @DisplayName("피드백을 저장하고 PairingFeedbackResult를 반환한다")
            void shouldSaveFeedbackAndReturnResult() {
                // given
                PairingFeedback savedFeedback = PairingFeedbackFixture.FRIEND_FORMAL.create();
                given(pairingFeedbackRepository.save(any(PairingFeedback.class)))
                        .willReturn(savedFeedback);

                // when
                PairingFeedbackResult result = pairingCommandService.submit(command());

                // then
                assertThat(result.id()).isEqualTo(1L);
                assertThat(result.memberId()).isEqualTo(MEMBER_ID);
                assertThat(result.drinkId()).isEqualTo(DRINK_ID);
                assertThat(result.anjuId()).isEqualTo(ANJU_ID);
                assertThat(result.musicMoodId()).isEqualTo(MUSIC_MOOD_ID);
                assertThat(result.rating()).isEqualTo(PairingRating.LIKE);
            }
        }

        private SubmitPairingFeedbackCommand command() {
            return new SubmitPairingFeedbackCommand(
                    MEMBER_ID,
                    DRINK_ID,
                    ANJU_ID,
                    MUSIC_MOOD_ID,
                    RelationshipType.FRIEND,
                    MoodType.FORMAL,
                    PairingRating.LIKE);
        }
    }
}
