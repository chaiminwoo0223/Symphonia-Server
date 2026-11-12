package com.symphonia.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.catalog.application.cache.CatalogCache;
import com.symphonia.catalog.application.dto.query.CatalogQuery;
import com.symphonia.catalog.application.dto.query.CatalogReferencesQuery;
import com.symphonia.catalog.application.dto.result.CatalogReferencesResult;
import com.symphonia.catalog.application.dto.result.CatalogResult;
import com.symphonia.catalog.application.dto.result.CatalogResult.AnjuResult;
import com.symphonia.catalog.application.dto.result.CatalogResult.DrinkResult;
import com.symphonia.catalog.application.usecase.RefreshCatalogCacheUseCase;
import com.symphonia.catalog.domain.exception.AnjuNotFoundException;
import com.symphonia.catalog.domain.exception.DrinkNotFoundException;
import com.symphonia.catalog.domain.repository.AnjuRepository;
import com.symphonia.catalog.domain.repository.DrinkRepository;
import com.symphonia.catalog.domain.repository.DrinkStyleRepository;
import com.symphonia.catalog.domain.vo.AnjuBase;
import com.symphonia.catalog.domain.vo.DrinkCategory;
import com.symphonia.catalog.domain.vo.ServingStyle;
import com.symphonia.catalog.fixture.AnjuFixture;
import com.symphonia.catalog.fixture.DrinkFixture;
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
    private static final Long WITHIN_ABV_DRINK_ID = 11L;
    private static final Long EQUAL_ABV_DRINK_ID = 12L;
    private static final Long OVER_ABV_DRINK_ID = 13L;

    @InjectMocks private CatalogQueryService catalogQueryService;

    @Mock private DrinkRepository drinkRepository;
    @Mock private DrinkStyleRepository drinkStyleRepository;
    @Mock private AnjuRepository anjuRepository;
    @Mock private CatalogCache catalogCache;
    @Mock private RefreshCatalogCacheUseCase refreshCatalogCacheUseCase;

    @Nested
    @DisplayName("get 메서드는")
    class Get {

        @Nested
        @DisplayName("캐시에 카탈로그가 있는 경우")
        class WhenCacheHit {

            @Test
            @DisplayName("DB를 읽지 않고 도수 상한 이하의 음료만 반환하며 상한과 같은 도수도 포함한다")
            void shouldReturnDrinksWithinMaxAbvWithoutReadingDb() {
                // given
                given(catalogCache.find()).willReturn(Optional.of(catalog()));

                // when
                CatalogResult result = catalogQueryService.get(CatalogQuery.of(MAX_ABV));

                // then
                assertThat(result.drinks())
                        .extracting(DrinkResult::id)
                        .containsExactly(WITHIN_ABV_DRINK_ID, EQUAL_ABV_DRINK_ID);
                assertThat(result.anjus()).hasSize(1);
                then(refreshCatalogCacheUseCase).shouldHaveNoInteractions();
                then(drinkRepository).shouldHaveNoInteractions();
            }

            @Test
            @DisplayName("상한이 Double.MAX_VALUE면 모든 음료를 반환한다")
            void shouldReturnAllDrinksWhenMaxAbvIsUnlimited() {
                // given
                given(catalogCache.find()).willReturn(Optional.of(catalog()));

                // when
                CatalogResult result = catalogQueryService.get(CatalogQuery.of(Double.MAX_VALUE));

                // then
                assertThat(result.drinks()).hasSize(3);
            }
        }

        @Nested
        @DisplayName("캐시에 카탈로그가 없는 경우")
        class WhenCacheMissed {

            @Test
            @DisplayName("DB에서 읽어 캐시를 채운 카탈로그에서 도수 상한 이하의 음료만 반환한다")
            void shouldReturnRefreshedCatalogWithinMaxAbv() {
                // given
                given(catalogCache.find()).willReturn(Optional.empty());
                given(refreshCatalogCacheUseCase.refresh()).willReturn(catalog());

                // when
                CatalogResult result = catalogQueryService.get(CatalogQuery.of(MAX_ABV));

                // then
                assertThat(result.drinks())
                        .extracting(DrinkResult::id)
                        .containsExactly(WITHIN_ABV_DRINK_ID, EQUAL_ABV_DRINK_ID);
            }
        }

        private CatalogResult catalog() {
            return new CatalogResult(
                    List.of(
                            drink(WITHIN_ABV_DRINK_ID, MAX_ABV - 1),
                            drink(EQUAL_ABV_DRINK_ID, MAX_ABV),
                            drink(OVER_ABV_DRINK_ID, MAX_ABV + 1)),
                    List.of(anju()));
        }

        private DrinkResult drink(Long id, double abv) {
            return new DrinkResult(id, 1L, DrinkCategory.BEER, "음료", abv, false, 1, 1, 1, 1, 1, 0L);
        }

        private AnjuResult anju() {
            return new AnjuResult(
                    ANJU_ID,
                    "안주",
                    1,
                    1,
                    0,
                    1,
                    1,
                    Set.of(),
                    AnjuBase.VEGETABLE,
                    ServingStyle.SNACK,
                    false,
                    0L);
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

        private CatalogReferencesQuery query() {
            return CatalogReferencesQuery.of(DRINK_ID, ANJU_ID);
        }
    }

    @Nested
    @DisplayName("getReferences 메서드는")
    class GetReferences {

        @Nested
        @DisplayName("Drink가 존재하지 않는 경우")
        class WhenDrinkNotFound {

            @Test
            @DisplayName("DrinkNotFoundException을 던진다")
            void shouldThrowDrinkNotFoundException() {
                // given
                given(drinkRepository.findById(DRINK_ID)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> catalogQueryService.getReferences(query()))
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
                        .willReturn(Optional.of(DrinkFixture.SOJU.createWithId(DRINK_ID)));
                given(anjuRepository.findById(ANJU_ID)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> catalogQueryService.getReferences(query()))
                        .isInstanceOf(AnjuNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("모든 대상이 존재하는 경우")
        class WhenReferencesExist {

            @Test
            @DisplayName("음료와 안주 이름을 CatalogReferencesResult로 반환한다")
            void shouldReturnCatalogReferencesResult() {
                // given
                given(drinkRepository.findById(DRINK_ID))
                        .willReturn(Optional.of(DrinkFixture.SOJU.createWithId(DRINK_ID)));
                given(anjuRepository.findById(ANJU_ID))
                        .willReturn(
                                Optional.of(AnjuFixture.GOLBAENGI_MUCHIM.createWithId(ANJU_ID)));

                // when
                CatalogReferencesResult result = catalogQueryService.getReferences(query());

                // then
                assertThat(result)
                        .isEqualTo(
                                new CatalogReferencesResult(
                                        DrinkFixture.SOJU.getName(),
                                        AnjuFixture.GOLBAENGI_MUCHIM.getName()));
            }
        }

        private CatalogReferencesQuery query() {
            return CatalogReferencesQuery.of(DRINK_ID, ANJU_ID);
        }
    }
}
