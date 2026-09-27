package com.symphonia.pairing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.symphonia.UnitTest;
import com.symphonia.pairing.application.dto.result.ImportResult;
import com.symphonia.pairing.application.dto.result.ImportResult.ExcludedItem;
import com.symphonia.pairing.domain.entity.DrinkStyle;
import com.symphonia.pairing.domain.reader.BjcpStyleReader;
import com.symphonia.pairing.domain.reader.FlavorRuleReader;
import com.symphonia.pairing.domain.repository.DrinkStyleRepository;
import com.symphonia.pairing.domain.vo.BjcpStyle;
import com.symphonia.pairing.domain.vo.DrinkCategory;
import com.symphonia.pairing.domain.vo.DrinkStyleSource;
import com.symphonia.pairing.domain.vo.FlavorAxis;
import com.symphonia.pairing.domain.vo.FlavorProfile;
import com.symphonia.pairing.fixture.BjcpStyleFixture;
import com.symphonia.pairing.fixture.DrinkStyleFixture;
import com.symphonia.pairing.fixture.FlavorRuleFixture;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("ImportDrinkStyleService 단위 테스트")
class ImportDrinkStyleServiceTest extends UnitTest {

    @InjectMocks private ImportDrinkStyleService importDrinkStyleService;

    @Mock private BjcpStyleReader bjcpStyleReader;
    @Mock private FlavorRuleReader flavorRuleReader;
    @Mock private DrinkStyleRepository drinkStyleRepository;

    @Nested
    @DisplayName("importBjcpStyles 메서드는")
    class ImportBjcpStyles {

        @BeforeEach
        void setUp() {
            given(flavorRuleReader.read()).willReturn(FlavorRuleFixture.STANDARD.create());
        }

        @Nested
        @DisplayName("처음 보는 스타일인 경우")
        class WhenStyleNotImported {

            @Test
            @DisplayName("source는 BJCP, externalId는 style_id, category는 BEER로 새로 저장한다")
            void shouldSaveNewDrinkStyle() {
                // given
                BjcpStyle style = BjcpStyleFixture.AMERICAN_LIGHT_LAGER.create();
                given(bjcpStyleReader.read()).willReturn(List.of(style));
                given(
                                drinkStyleRepository.findBySourceAndExternalId(
                                        DrinkStyleSource.BJCP, style.styleId()))
                        .willReturn(Optional.empty());

                // when
                ImportResult result = importDrinkStyleService.importBjcpStyles();

                // then
                DrinkStyle saved = captureSavedDrinkStyle();
                assertThat(saved.getId()).isNull();
                assertThat(saved.getSource()).isEqualTo(DrinkStyleSource.BJCP);
                assertThat(saved.getExternalId()).isEqualTo(style.styleId());
                assertThat(saved.getName()).isEqualTo(style.name());
                assertThat(saved.getCategory()).isEqualTo(DrinkCategory.BEER);
                assertThat(saved.getFlavorProfile())
                        .usingRecursiveComparison()
                        .isEqualTo(FlavorProfile.of(1, 2, 3, 3, 1));
                assertThat(result.importedCount()).isEqualTo(1);
            }
        }

        @Nested
        @DisplayName("이미 적재된 스타일인 경우")
        class WhenStyleAlreadyImported {

            @Test
            @DisplayName("id를 유지한 채 이름과 맛을 갱신해 저장한다")
            void shouldUpdateExistingDrinkStyle() {
                // given
                BjcpStyle style = BjcpStyleFixture.AMERICAN_LIGHT_LAGER.createWithTags("sour");
                DrinkStyle existing =
                        DrinkStyleFixture.ALE.createWithIdAndIdentity(
                                DrinkStyleSource.BJCP, style.styleId());
                Long existingId = existing.getId();
                given(bjcpStyleReader.read()).willReturn(List.of(style));
                given(
                                drinkStyleRepository.findBySourceAndExternalId(
                                        DrinkStyleSource.BJCP, style.styleId()))
                        .willReturn(Optional.of(existing));

                // when
                ImportResult result = importDrinkStyleService.importBjcpStyles();

                // then
                DrinkStyle saved = captureSavedDrinkStyle();
                assertThat(saved.getId()).isEqualTo(existingId);
                assertThat(saved.getSource()).isEqualTo(DrinkStyleSource.BJCP);
                assertThat(saved.getExternalId()).isEqualTo(style.styleId());
                assertThat(saved.getName()).isEqualTo(style.name());
                assertThat(saved.getFlavorProfile())
                        .usingRecursiveComparison()
                        .isEqualTo(FlavorProfile.of(1, 2, 3, 3, 4));
                assertThat(result.importedCount()).isEqualTo(1);
            }
        }

        @Nested
        @DisplayName("맛을 정할 수 없는 스타일인 경우")
        class WhenFlavorUndetermined {

            @Test
            @DisplayName("저장하지 않고 규칙이 없는 축과 함께 excludedItems에 담는다")
            void shouldExcludeStyleWithoutSaving() {
                // given
                BjcpStyle style = BjcpStyleFixture.COMMERCIAL_SPECIALTY_BEER.create();
                given(bjcpStyleReader.read()).willReturn(List.of(style));

                // when
                ImportResult result = importDrinkStyleService.importBjcpStyles();

                // then
                verify(drinkStyleRepository, never()).save(any());
                assertThat(result.importedCount()).isZero();
                assertThat(result.excludedItems())
                        .containsExactly(
                                new ExcludedItem(
                                        style.styleId(),
                                        style.name(),
                                        EnumSet.of(
                                                FlavorAxis.BITTERNESS,
                                                FlavorAxis.CARBONATION,
                                                FlavorAxis.RICHNESS)));
                assertThat(result.defaultedExternalIds()).isEmpty();
            }
        }

        @Nested
        @DisplayName("여러 스타일을 함께 적재하는 경우")
        class WhenMultipleStyles {

            @Test
            @DisplayName("적재한 수를 세고 기본값을 쓴 축별로 style_id를 모은다")
            void shouldCountImportedStylesAndCollectDefaultedStyleIds() {
                // given
                BjcpStyle noTags = BjcpStyleFixture.AMERICAN_LIGHT_LAGER.create();
                BjcpStyle sour = BjcpStyleFixture.BERLINER_WEISSE.create();
                BjcpStyle sweetSour = BjcpStyleFixture.FRUIT_LAMBIC.create();
                BjcpStyle undetermined = BjcpStyleFixture.COMMERCIAL_SPECIALTY_BEER.create();
                given(bjcpStyleReader.read())
                        .willReturn(List.of(noTags, sour, sweetSour, undetermined));
                given(drinkStyleRepository.findBySourceAndExternalId(any(), any()))
                        .willReturn(Optional.empty());

                // when
                ImportResult result = importDrinkStyleService.importBjcpStyles();

                // then
                assertThat(result.importedCount()).isEqualTo(3);
                assertThat(result.excludedItems())
                        .extracting(ExcludedItem::externalId)
                        .containsExactly(undetermined.styleId());
                assertThat(result.defaultedExternalIds())
                        .containsOnlyKeys(FlavorAxis.SWEETNESS, FlavorAxis.ACIDITY);
                assertThat(result.defaultedExternalIds().get(FlavorAxis.SWEETNESS))
                        .containsExactly(noTags.styleId(), sour.styleId());
                assertThat(result.defaultedExternalIds().get(FlavorAxis.ACIDITY))
                        .containsExactly(noTags.styleId());
            }
        }
    }

    private DrinkStyle captureSavedDrinkStyle() {
        ArgumentCaptor<DrinkStyle> captor = ArgumentCaptor.forClass(DrinkStyle.class);
        verify(drinkStyleRepository).save(captor.capture());
        return captor.getValue();
    }
}
