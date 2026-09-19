package com.symphonia.pairing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.symphonia.UnitTest;
import com.symphonia.pairing.application.dto.command.SubmitPairingFeedbackCommand;
import com.symphonia.pairing.application.dto.result.PairingFeedbackResult;
import com.symphonia.pairing.domain.entity.Anju;
import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.entity.MusicMood;
import com.symphonia.pairing.domain.entity.PairingFeedback;
import com.symphonia.pairing.domain.exception.AnjuNotFoundException;
import com.symphonia.pairing.domain.exception.DrinkNotFoundException;
import com.symphonia.pairing.domain.exception.MusicMoodNotFoundException;
import com.symphonia.pairing.domain.repository.AnjuRepository;
import com.symphonia.pairing.domain.repository.DrinkRepository;
import com.symphonia.pairing.domain.repository.MusicMoodRepository;
import com.symphonia.pairing.domain.repository.PairingFeedbackRepository;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.RelationshipType;
import com.symphonia.pairing.fixture.AnjuFixture;
import com.symphonia.pairing.fixture.DrinkFixture;
import com.symphonia.pairing.fixture.MusicMoodFixture;
import com.symphonia.pairing.fixture.PairingFeedbackFixture;
import java.util.Optional;
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

    @Mock private DrinkRepository drinkRepository;
    @Mock private AnjuRepository anjuRepository;
    @Mock private MusicMoodRepository musicMoodRepository;
    @Mock private PairingFeedbackRepository pairingFeedbackRepository;

    @Nested
    @DisplayName("submit 메서드는")
    class Submit {

        @Nested
        @DisplayName("Drink가 존재하지 않는 경우")
        class WhenDrinkNotFound {

            @Test
            @DisplayName("DrinkNotFoundException을 던진다")
            void shouldThrowDrinkNotFoundException() {
                // given
                given(drinkRepository.findById(DRINK_ID)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> pairingCommandService.submit(command()))
                        .isInstanceOf(DrinkNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("Anju가 존재하지 않는 경우")
        class WhenAnjuNotFound {

            @Test
            @DisplayName("AnjuNotFoundException을 던진다")
            void shouldThrowAnjuNotFoundException() {
                // given
                Drink drink = DrinkFixture.SOJU.create();
                given(drinkRepository.findById(DRINK_ID)).willReturn(Optional.of(drink));
                given(anjuRepository.findById(ANJU_ID)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> pairingCommandService.submit(command()))
                        .isInstanceOf(AnjuNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("MusicMood가 존재하지 않는 경우")
        class WhenMusicMoodNotFound {

            @Test
            @DisplayName("MusicMoodNotFoundException을 던진다")
            void shouldThrowMusicMoodNotFoundException() {
                // given
                Drink drink = DrinkFixture.SOJU.create();
                Anju anju = AnjuFixture.GOLBAENGI_MUCHIM.create();
                given(drinkRepository.findById(DRINK_ID)).willReturn(Optional.of(drink));
                given(anjuRepository.findById(ANJU_ID)).willReturn(Optional.of(anju));
                given(musicMoodRepository.findById(MUSIC_MOOD_ID)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> pairingCommandService.submit(command()))
                        .isInstanceOf(MusicMoodNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("모두 존재하는 경우")
        class WhenAllFound {

            @Test
            @DisplayName("피드백을 저장하고 PairingFeedbackResult를 반환한다")
            void shouldSaveFeedbackAndReturnResult() {
                // given
                Drink drink = DrinkFixture.SOJU.create();
                Anju anju = AnjuFixture.GOLBAENGI_MUCHIM.create();
                MusicMood musicMood = MusicMoodFixture.FORMAL_JAZZ.create();
                given(drinkRepository.findById(DRINK_ID)).willReturn(Optional.of(drink));
                given(anjuRepository.findById(ANJU_ID)).willReturn(Optional.of(anju));
                given(musicMoodRepository.findById(MUSIC_MOOD_ID))
                        .willReturn(Optional.of(musicMood));

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
            }
        }

        private SubmitPairingFeedbackCommand command() {
            return new SubmitPairingFeedbackCommand(
                    MEMBER_ID,
                    DRINK_ID,
                    ANJU_ID,
                    MUSIC_MOOD_ID,
                    RelationshipType.FRIEND,
                    MoodType.FORMAL);
        }
    }
}
