package com.symphonia.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.symphonia.UnitTest;
import com.symphonia.catalog.application.cache.CatalogCache;
import com.symphonia.catalog.application.dto.result.CatalogResult;
import com.symphonia.catalog.application.dto.result.CatalogResult.AnjuResult;
import com.symphonia.catalog.application.dto.result.CatalogResult.DrinkResult;
import com.symphonia.catalog.domain.entity.Anju;
import com.symphonia.catalog.domain.entity.Drink;
import com.symphonia.catalog.domain.repository.AnjuRepository;
import com.symphonia.catalog.domain.repository.DrinkRepository;
import com.symphonia.catalog.domain.repository.DrinkStyleRepository;
import com.symphonia.catalog.domain.vo.FlavorProfile;
import com.symphonia.catalog.domain.vo.Harmonize;
import com.symphonia.catalog.fixture.AnjuFixture;
import com.symphonia.catalog.fixture.DrinkFixture;
import com.symphonia.catalog.fixture.DrinkStyleFixture;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("RefreshCatalogCacheService 단위 테스트")
class RefreshCatalogCacheServiceTest extends UnitTest {

    private static final Long DRINK_ID = 1L;
    private static final Long ANJU_ID = 2L;

    @InjectMocks private RefreshCatalogCacheService refreshCatalogCacheService;

    @Mock private DrinkRepository drinkRepository;
    @Mock private DrinkStyleRepository drinkStyleRepository;
    @Mock private AnjuRepository anjuRepository;
    @Mock private CatalogCache catalogCache;

    @Nested
    @DisplayName("refresh 메서드는")
    class Refresh {

        @Test
        @DisplayName("음료를 스타일의 종류와 함께, 안주를 분류 값과 함께 CatalogResult로 반환한다")
        void shouldReturnCatalogResultWithinMaxAbv() {
            // given: 축마다 값이 달라 축 순서가 바뀌면 결과가 달라진다
            Drink beer =
                    DrinkFixture.BEER.createWithIdAndFlavorProfile(
                            DRINK_ID, FlavorProfile.of(1, 2, 3, 4, 5));
            Anju anju =
                    AnjuFixture.DRIED_SNACK.createWithIdAndFlavorProfile(
                            ANJU_ID, FlavorProfile.of(5, 4, 0, 2, 1));
            given(drinkRepository.findAll()).willReturn(List.of(beer));
            given(drinkStyleRepository.findAll())
                    .willReturn(List.of(DrinkStyleFixture.ALE.createWithId()));
            given(anjuRepository.findAll()).willReturn(List.of(anju));

            // when
            CatalogResult result = refreshCatalogCacheService.refresh();

            // then
            assertThat(result.drinks())
                    .containsExactly(
                            new DrinkResult(
                                    DRINK_ID,
                                    beer.getDrinkStyleId(),
                                    DrinkStyleFixture.ALE.getCategory(),
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
                                    AnjuFixture.DRIED_SNACK.getBase(),
                                    AnjuFixture.DRIED_SNACK.getServing(),
                                    AnjuFixture.DRIED_SNACK.isSpicy(),
                                    anju.getHarmonizeMask()));
        }

        @Test
        @DisplayName("스타일을 찾지 못한 음료는 종류 없이 반환한다")
        void shouldReturnDrinkWithoutCategoryWhenDrinkStyleNotFound() {
            // given
            given(drinkRepository.findAll())
                    .willReturn(List.of(DrinkFixture.BEER.createWithId(DRINK_ID)));
            given(anjuRepository.findAll()).willReturn(List.of());

            // when
            CatalogResult result = refreshCatalogCacheService.refresh();

            // then
            assertThat(result.drinks()).singleElement().extracting(DrinkResult::category).isNull();
        }

        @Test
        @DisplayName("음식 계열이 맞는 음료와 안주는 harmonizeMask가 겹친다")
        void shouldReturnOverlappingHarmonizeMasksWhenHarmonized() {
            // given
            Drink drink = DrinkFixture.SOJU.createWithHarmonizes(Set.of(Harmonize.PORK));
            Anju anju = AnjuFixture.TOFU_KIMCHI.createWithId();
            given(drinkRepository.findAll()).willReturn(List.of(drink));
            given(anjuRepository.findAll()).willReturn(List.of(anju));

            // when
            CatalogResult result = refreshCatalogCacheService.refresh();

            // then
            assertThat(
                            result.drinks().getFirst().harmonizeMask()
                                    & result.anjus().getFirst().harmonizeMask())
                    .isNotZero();
        }

        @Test
        @DisplayName("읽은 카탈로그를 캐시에 덮어쓴다")
        void shouldPutCatalogToCache() {
            // given
            given(drinkRepository.findAll())
                    .willReturn(List.of(DrinkFixture.BEER.createWithId(DRINK_ID)));
            given(anjuRepository.findAll()).willReturn(List.of());

            // when
            CatalogResult result = refreshCatalogCacheService.refresh();

            // then
            then(catalogCache).should().put(result);
        }
    }
}
