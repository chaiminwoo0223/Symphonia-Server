package com.symphonia.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.symphonia.UnitTest;
import com.symphonia.catalog.application.dto.query.CatalogQuery;
import com.symphonia.catalog.application.dto.query.VerifyCatalogReferencesQuery;
import com.symphonia.catalog.application.dto.result.CatalogResult;
import com.symphonia.catalog.application.dto.result.CatalogResult.AnjuResult;
import com.symphonia.catalog.application.dto.result.CatalogResult.DrinkResult;
import com.symphonia.catalog.application.dto.result.CatalogResult.MusicMoodResult;
import com.symphonia.catalog.domain.entity.Anju;
import com.symphonia.catalog.domain.entity.Drink;
import com.symphonia.catalog.domain.entity.MusicMood;
import com.symphonia.catalog.domain.exception.AnjuNotFoundException;
import com.symphonia.catalog.domain.exception.DrinkNotFoundException;
import com.symphonia.catalog.domain.exception.MusicMoodNotFoundException;
import com.symphonia.catalog.domain.repository.AnjuRepository;
import com.symphonia.catalog.domain.repository.DrinkRepository;
import com.symphonia.catalog.domain.repository.MusicMoodRepository;
import com.symphonia.catalog.domain.vo.FlavorProfile;
import com.symphonia.catalog.domain.vo.Harmonize;
import com.symphonia.catalog.domain.vo.MoodProfile;
import com.symphonia.catalog.fixture.AnjuFixture;
import com.symphonia.catalog.fixture.DrinkFixture;
import com.symphonia.catalog.fixture.MusicMoodFixture;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("CatalogQueryService 단위 테스트")
class CatalogQueryServiceTest extends UnitTest {

    private static final double MAX_ABV = 12.0;
    private static final Long DRINK_ID = 1L;
    private static final Long ANJU_ID = 2L;
    private static final Long MUSIC_MOOD_ID = 3L;

    @InjectMocks private CatalogQueryService catalogQueryService;

    @Mock private DrinkRepository drinkRepository;
    @Mock private AnjuRepository anjuRepository;
    @Mock private MusicMoodRepository musicMoodRepository;

    @Nested
    @DisplayName("get 메서드는")
    class Get {

        @Test
        @DisplayName("도수 상한 이하의 음료와 전체 안주, 음악 무드를 CatalogResult로 반환한다")
        void shouldReturnCatalogResultWithinMaxAbv() {
            // given: 축마다 값이 달라 축 순서가 바뀌면 결과가 달라진다
            Drink beer =
                    DrinkFixture.BEER.createWithIdAndFlavorProfile(
                            DRINK_ID, FlavorProfile.of(1, 2, 3, 4, 5));
            Anju anju =
                    AnjuFixture.DRIED_SNACK.createWithIdAndFlavorProfile(
                            ANJU_ID, FlavorProfile.of(5, 4, 0, 2, 1));
            MusicMood musicMood =
                    MusicMoodFixture.FORMAL_JAZZ.createWithIdAndMoodProfile(
                            MUSIC_MOOD_ID, MoodProfile.of(1, 2, 3, 4));
            given(drinkRepository.findAllByAbvLessThanEqual(MAX_ABV)).willReturn(List.of(beer));
            given(anjuRepository.findAll()).willReturn(List.of(anju));
            given(musicMoodRepository.findAll()).willReturn(List.of(musicMood));

            // when
            CatalogResult result = catalogQueryService.get(CatalogQuery.of(MAX_ABV));

            // then
            assertThat(result.drinks())
                    .containsExactly(
                            new DrinkResult(
                                    DRINK_ID,
                                    beer.getDrinkStyleId(),
                                    DrinkFixture.BEER.getName(),
                                    DrinkFixture.BEER.getAbv(),
                                    DrinkFixture.BEER.isNonAlcoholic(),
                                    1,
                                    2,
                                    3,
                                    4,
                                    5,
                                    beer.getHarmonizeMask()));
            assertThat(result.anjus())
                    .containsExactly(
                            new AnjuResult(
                                    ANJU_ID,
                                    AnjuFixture.DRIED_SNACK.getName(),
                                    5,
                                    4,
                                    0,
                                    2,
                                    1,
                                    AnjuFixture.DRIED_SNACK.getAllergyTypes(),
                                    anju.getHarmonizeMask()));
            assertThat(result.musicMoods())
                    .containsExactly(
                            new MusicMoodResult(
                                    MUSIC_MOOD_ID,
                                    MusicMoodFixture.FORMAL_JAZZ.getTitle(),
                                    MusicMoodFixture.FORMAL_JAZZ.getStreamingUrl(),
                                    1,
                                    2,
                                    3,
                                    4));
        }

        @Test
        @DisplayName("음식 계열이 맞는 음료와 안주는 harmonizeMask가 겹친다")
        void shouldReturnOverlappingHarmonizeMasksWhenHarmonized() {
            // given
            Drink drink = DrinkFixture.SOJU.createWithHarmonizes(Set.of(Harmonize.PORK));
            Anju anju = AnjuFixture.TOFU_KIMCHI.createWithId();
            given(drinkRepository.findAllByAbvLessThanEqual(MAX_ABV)).willReturn(List.of(drink));
            given(anjuRepository.findAll()).willReturn(List.of(anju));
            given(musicMoodRepository.findAll()).willReturn(List.of());

            // when
            CatalogResult result = catalogQueryService.get(CatalogQuery.of(MAX_ABV));

            // then
            assertThat(
                            result.drinks().getFirst().harmonizeMask()
                                    & result.anjus().getFirst().harmonizeMask())
                    .isNotZero();
        }
    }

    @Nested
    @DisplayName("verify 메서드는")
    class Verify {

        @Nested
        @DisplayName("Drink가 존재하지 않는 경우")
        class WhenDrinkNotFound {

            @Test
            @DisplayName("DrinkNotFoundException을 던진다")
            void shouldThrowDrinkNotFoundException() {
                // given
                given(drinkRepository.findById(DRINK_ID)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> catalogQueryService.verify(query()))
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
                given(drinkRepository.findById(DRINK_ID))
                        .willReturn(Optional.of(DrinkFixture.SOJU.create()));
                given(anjuRepository.findById(ANJU_ID)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> catalogQueryService.verify(query()))
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
                given(drinkRepository.findById(DRINK_ID))
                        .willReturn(Optional.of(DrinkFixture.SOJU.create()));
                given(anjuRepository.findById(ANJU_ID))
                        .willReturn(Optional.of(AnjuFixture.GOLBAENGI_MUCHIM.create()));
                given(musicMoodRepository.findById(MUSIC_MOOD_ID)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> catalogQueryService.verify(query()))
                        .isInstanceOf(MusicMoodNotFoundException.class);
            }
        }

        private VerifyCatalogReferencesQuery query() {
            return VerifyCatalogReferencesQuery.of(DRINK_ID, ANJU_ID, MUSIC_MOOD_ID);
        }
    }
}
