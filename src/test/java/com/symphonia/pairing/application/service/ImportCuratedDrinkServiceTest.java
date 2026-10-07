package com.symphonia.pairing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.symphonia.UnitTest;
import com.symphonia.pairing.application.dto.result.ImportCuratedDrinkResult;
import com.symphonia.pairing.application.dto.result.ImportCuratedDrinkResult.UnmappedItem;
import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.entity.DrinkStyle;
import com.symphonia.pairing.domain.reader.CuratedDrinkReader;
import com.symphonia.pairing.domain.repository.DrinkRepository;
import com.symphonia.pairing.domain.repository.DrinkStyleRepository;
import com.symphonia.pairing.domain.vo.CuratedDrink;
import com.symphonia.pairing.domain.vo.DrinkSource;
import com.symphonia.pairing.domain.vo.DrinkStyleSource;
import com.symphonia.pairing.fixture.CuratedDrinkFixture;
import com.symphonia.pairing.fixture.DrinkStyleFixture;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("ImportCuratedDrinkService 단위 테스트")
class ImportCuratedDrinkServiceTest extends UnitTest {

    @InjectMocks private ImportCuratedDrinkService importCuratedDrinkService;

    @Mock private CuratedDrinkReader curatedDrinkReader;
    @Mock private DrinkStyleRepository drinkStyleRepository;
    @Mock private DrinkRepository drinkRepository;

    @Nested
    @DisplayName("importCuratedBeers 메서드는")
    class ImportCuratedBeers {

        @Nested
        @DisplayName("처음 보는 제품인 경우")
        class WhenDrinkNotImported {

            @Test
            @DisplayName("source는 CURATED, externalId는 매핑표 값, 스타일의 id와 맛을 복사해 새로 저장한다")
            void shouldSaveNewDrinkWithMappedStyle() {
                // given
                CuratedDrink curatedDrink = CuratedDrinkFixture.CASS_FRESH.create();
                DrinkStyle mappedStyle = givenMappedStyle(curatedDrink);
                given(curatedDrinkReader.read()).willReturn(List.of(curatedDrink));
                given(
                                drinkRepository.findBySourceAndExternalId(
                                        DrinkSource.CURATED, curatedDrink.externalId()))
                        .willReturn(Optional.empty());

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
                assertThat(result.importedCount()).isEqualTo(1);
                assertThat(result.unmappedItems()).isEmpty();
            }
        }

        @Nested
        @DisplayName("이미 적재된 제품인 경우")
        class WhenDrinkAlreadyImported {

            @Test
            @DisplayName("id를 유지한 채 스타일, 이름, 도수, 맛을 갱신해 저장한다")
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
                given(
                                drinkRepository.findBySourceAndExternalId(
                                        DrinkSource.CURATED, curatedDrink.externalId()))
                        .willReturn(Optional.of(existing));

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
                assertThat(result.importedCount()).isEqualTo(1);
            }
        }

        @Nested
        @DisplayName("매핑한 BJCP 스타일이 적재되지 않은 경우")
        class WhenMappedStyleNotFound {

            @Test
            @DisplayName("저장하지 않고 unmappedItems에 담는다")
            void shouldLeaveUnmappedItemWithoutSaving() {
                // given
                CuratedDrink curatedDrink = CuratedDrinkFixture.HOEGAARDEN.create();
                given(curatedDrinkReader.read()).willReturn(List.of(curatedDrink));
                given(
                                drinkStyleRepository.findBySourceAndExternalId(
                                        DrinkStyleSource.BJCP, curatedDrink.bjcpStyleId()))
                        .willReturn(Optional.empty());

                // when
                ImportCuratedDrinkResult result = importCuratedDrinkService.importCuratedBeers();

                // then
                verify(drinkRepository, never()).save(any());
                assertThat(result.importedCount()).isZero();
                assertThat(result.unmappedItems())
                        .containsExactly(
                                new UnmappedItem(
                                        curatedDrink.externalId(),
                                        curatedDrink.name(),
                                        curatedDrink.bjcpStyleId()));
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
                givenMappedStyle(mapped);
                given(
                                drinkStyleRepository.findBySourceAndExternalId(
                                        DrinkStyleSource.BJCP, unmapped.bjcpStyleId()))
                        .willReturn(Optional.empty());
                given(curatedDrinkReader.read()).willReturn(List.of(mapped, unmapped));
                given(drinkRepository.findBySourceAndExternalId(any(), any()))
                        .willReturn(Optional.empty());

                // when
                ImportCuratedDrinkResult result = importCuratedDrinkService.importCuratedBeers();

                // then
                assertThat(captureSavedDrink().getExternalId()).isEqualTo(mapped.externalId());
                assertThat(result.importedCount()).isEqualTo(1);
                assertThat(result.unmappedItems())
                        .extracting(UnmappedItem::externalId)
                        .containsExactly(unmapped.externalId());
            }
        }
    }

    private DrinkStyle givenMappedStyle(CuratedDrink curatedDrink) {
        DrinkStyle mappedStyle =
                DrinkStyleFixture.ALE.createWithIdAndIdentity(
                        DrinkStyleSource.BJCP, curatedDrink.bjcpStyleId());
        given(
                        drinkStyleRepository.findBySourceAndExternalId(
                                DrinkStyleSource.BJCP, curatedDrink.bjcpStyleId()))
                .willReturn(Optional.of(mappedStyle));
        return mappedStyle;
    }

    private Drink captureSavedDrink() {
        ArgumentCaptor<Drink> captor = ArgumentCaptor.forClass(Drink.class);
        verify(drinkRepository).save(captor.capture());
        return captor.getValue();
    }
}
