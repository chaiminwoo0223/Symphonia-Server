package com.symphonia.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.symphonia.UnitTest;
import com.symphonia.catalog.application.dto.result.ImportCuratedDrinkResult;
import com.symphonia.catalog.application.dto.result.ImportCuratedDrinkResult.UnmappedDrink;
import com.symphonia.catalog.domain.entity.Drink;
import com.symphonia.catalog.domain.entity.DrinkStyle;
import com.symphonia.catalog.domain.reader.BeerHarmonizeRuleReader;
import com.symphonia.catalog.domain.reader.CuratedDrinkReader;
import com.symphonia.catalog.domain.repository.DrinkRepository;
import com.symphonia.catalog.domain.repository.DrinkStyleRepository;
import com.symphonia.catalog.domain.vo.BeerHarmonizeRule;
import com.symphonia.catalog.domain.vo.CuratedDrink;
import com.symphonia.catalog.domain.vo.DrinkSource;
import com.symphonia.catalog.domain.vo.DrinkStyleSource;
import com.symphonia.catalog.domain.vo.Harmonize;
import com.symphonia.catalog.fixture.CuratedDrinkFixture;
import com.symphonia.catalog.fixture.DrinkStyleFixture;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("ImportCuratedDrinkService 단위 테스트")
class ImportCuratedDrinkServiceTest extends UnitTest {

    // 규칙표에는 CASS_FRESH의 스타일(2A)만 두어, HOEGAARDEN의 스타일(24A)은 음식 계열 규칙이 없는 상황을 만든다.
    private static final Set<Harmonize> LAGER_HARMONIZES =
            Set.of(Harmonize.POULTRY, Harmonize.VEGETARIAN);

    @InjectMocks private ImportCuratedDrinkService importCuratedDrinkService;

    @Mock private CuratedDrinkReader curatedDrinkReader;
    @Mock private BeerHarmonizeRuleReader beerHarmonizeRuleReader;
    @Mock private DrinkStyleRepository drinkStyleRepository;
    @Mock private DrinkRepository drinkRepository;

    @Nested
    @DisplayName("importCuratedBeers 메서드는")
    class ImportCuratedBeers {

        @BeforeEach
        void setUp() {
            given(beerHarmonizeRuleReader.read())
                    .willReturn(List.of(new BeerHarmonizeRule("2A", LAGER_HARMONIZES)));
        }

        @Nested
        @DisplayName("처음 보는 제품인 경우")
        class WhenDrinkNotImported {

            @Test
            @DisplayName("source는 CURATED, externalId는 매핑표 값, 스타일의 id와 맛, 음식 계열을 복사해 새로 저장한다")
            void shouldSaveNewDrinkWithMappedStyle() {
                // given
                CuratedDrink curatedDrink = CuratedDrinkFixture.CASS_FRESH.create();
                DrinkStyle mappedStyle = givenMappedStyle(curatedDrink);
                given(curatedDrinkReader.read()).willReturn(List.of(curatedDrink));
                given(drinkRepository.findAllBySource(DrinkSource.CURATED)).willReturn(List.of());

                // when
                ImportCuratedDrinkResult result = importCuratedDrinkService.importCuratedBeers();

                // then
                Drink saved = captureSavedDrink();
                assertThat(saved.getId()).isNull();
                assertThat(saved.getSource()).isEqualTo(DrinkSource.CURATED);
                assertThat(saved.getExternalId()).isEqualTo(curatedDrink.externalId());
                assertThat(saved.getName()).isEqualTo(curatedDrink.name());
                assertThat(saved.getAbv()).isEqualTo(curatedDrink.abv());
                assertThat(saved.isNonAlcoholic()).isFalse();
                assertThat(saved.getDrinkStyleId()).isEqualTo(mappedStyle.getId());
                assertThat(saved.getFlavorProfile())
                        .usingRecursiveComparison()
                        .isEqualTo(mappedStyle.getFlavorProfile());
                assertThat(saved.getHarmonizes()).isEqualTo(LAGER_HARMONIZES);
                assertThat(result.importedCount()).isEqualTo(1);
                assertThat(result.unmappedDrinks()).isEmpty();
            }
        }

        @Nested
        @DisplayName("이미 적재된 제품인 경우")
        class WhenDrinkAlreadyImported {

            @Test
            @DisplayName("id를 유지한 채 스타일, 이름, 도수, 맛, 음식 계열을 갱신해 저장한다")
            void shouldUpdateExistingDrink() {
                // given
                CuratedDrink curatedDrink =
                        CuratedDrinkFixture.CASS_FRESH.createWithNameAndAbv("카스 라이트", 4.0);
                DrinkStyle mappedStyle = givenMappedStyle(curatedDrink);
                DrinkStyle previousStyle =
                        DrinkStyleFixture.PINOT_NOIR.createWithIdAndIdentity(
                                DrinkStyleSource.BJCP, "1B");
                Drink existing =
                        CuratedDrinkFixture.CASS_FRESH.createImportedDrink(10L, previousStyle);
                given(curatedDrinkReader.read()).willReturn(List.of(curatedDrink));
                given(drinkRepository.findAllBySource(DrinkSource.CURATED))
                        .willReturn(List.of(existing));

                // when
                ImportCuratedDrinkResult result = importCuratedDrinkService.importCuratedBeers();

                // then
                Drink saved = captureSavedDrink();
                assertThat(saved.getId()).isEqualTo(10L);
                assertThat(saved.getSource()).isEqualTo(DrinkSource.CURATED);
                assertThat(saved.getExternalId()).isEqualTo(curatedDrink.externalId());
                assertThat(saved.getName()).isEqualTo("카스 라이트");
                assertThat(saved.getAbv()).isEqualTo(4.0);
                assertThat(saved.getDrinkStyleId()).isEqualTo(mappedStyle.getId());
                assertThat(saved.getFlavorProfile())
                        .usingRecursiveComparison()
                        .isEqualTo(mappedStyle.getFlavorProfile());
                assertThat(saved.getHarmonizes()).isEqualTo(LAGER_HARMONIZES);
                assertThat(result.importedCount()).isEqualTo(1);
            }
        }

        @Nested
        @DisplayName("매핑한 BJCP 스타일이 적재되지 않은 경우")
        class WhenMappedStyleNotFound {

            @Test
            @DisplayName("저장하지 않고 unmappedDrinks에 담는다")
            void shouldLeaveUnmappedDrinkWithoutSaving() {
                // given
                CuratedDrink curatedDrink = CuratedDrinkFixture.HOEGAARDEN.create();
                given(curatedDrinkReader.read()).willReturn(List.of(curatedDrink));
                given(drinkStyleRepository.findAllBySource(DrinkStyleSource.BJCP))
                        .willReturn(List.of());

                // when
                ImportCuratedDrinkResult result = importCuratedDrinkService.importCuratedBeers();

                // then
                verify(drinkRepository, never()).save(any());
                assertThat(result.importedCount()).isZero();
                assertThat(result.unmappedDrinks())
                        .containsExactly(
                                new UnmappedDrink(
                                        curatedDrink.externalId(),
                                        curatedDrink.name(),
                                        curatedDrink.bjcpStyleId()));
            }
        }

        @Nested
        @DisplayName("매핑한 BJCP 스타일의 음식 계열 규칙이 없는 경우")
        class WhenHarmonizeRuleNotFound {

            @Test
            @DisplayName("스타일이 적재되어 있어도 저장하지 않고 unmappedDrinks에 담는다")
            void shouldLeaveUnmappedDrinkWithoutSaving() {
                // given
                CuratedDrink curatedDrink = CuratedDrinkFixture.HOEGAARDEN.create();
                givenMappedStyle(curatedDrink);
                given(curatedDrinkReader.read()).willReturn(List.of(curatedDrink));

                // when
                ImportCuratedDrinkResult result = importCuratedDrinkService.importCuratedBeers();

                // then
                verify(drinkRepository, never()).save(any());
                assertThat(result.importedCount()).isZero();
                assertThat(result.unmappedDrinks())
                        .extracting(UnmappedDrink::externalId)
                        .containsExactly(curatedDrink.externalId());
            }
        }

        @Nested
        @DisplayName("매핑된 제품과 매핑되지 않은 제품이 섞인 경우")
        class WhenMappedAndUnmappedDrinksMixed {

            @Test
            @DisplayName("매핑된 제품만 저장하고 전체에서 매핑되지 않은 수를 뺀 만큼 적재 수로 센다")
            void shouldCountOnlyMappedDrinks() {
                // given
                CuratedDrink mapped = CuratedDrinkFixture.CASS_FRESH.create();
                CuratedDrink unmapped = CuratedDrinkFixture.HOEGAARDEN.create();
                // 적재된 BJCP 스타일에는 mapped의 스타일만 있고 unmapped의 스타일은 없다.
                givenMappedStyle(mapped);
                given(curatedDrinkReader.read()).willReturn(List.of(mapped, unmapped));
                given(drinkRepository.findAllBySource(DrinkSource.CURATED)).willReturn(List.of());

                // when
                ImportCuratedDrinkResult result = importCuratedDrinkService.importCuratedBeers();

                // then
                assertThat(captureSavedDrink().getExternalId()).isEqualTo(mapped.externalId());
                assertThat(result.importedCount()).isEqualTo(1);
                assertThat(result.unmappedDrinks())
                        .extracting(UnmappedDrink::externalId)
                        .containsExactly(unmapped.externalId());
            }
        }
    }

    private DrinkStyle givenMappedStyle(CuratedDrink curatedDrink) {
        DrinkStyle mappedStyle =
                DrinkStyleFixture.ALE.createWithIdAndIdentity(
                        DrinkStyleSource.BJCP, curatedDrink.bjcpStyleId());
        given(drinkStyleRepository.findAllBySource(DrinkStyleSource.BJCP))
                .willReturn(List.of(mappedStyle));
        return mappedStyle;
    }

    private Drink captureSavedDrink() {
        ArgumentCaptor<Drink> captor = ArgumentCaptor.forClass(Drink.class);
        verify(drinkRepository).save(captor.capture());
        return captor.getValue();
    }
}
