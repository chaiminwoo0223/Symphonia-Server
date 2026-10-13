package com.symphonia.pairing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.symphonia.UnitTest;
import com.symphonia.pairing.application.dto.result.ImportXWineResult;
import com.symphonia.pairing.application.dto.result.ImportXWineResult.AbvExceededItem;
import com.symphonia.pairing.application.dto.result.ImportXWineResult.UndeterminedItem;
import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.entity.DrinkStyle;
import com.symphonia.pairing.domain.reader.WineFlavorRuleReader;
import com.symphonia.pairing.domain.reader.XWineReader;
import com.symphonia.pairing.domain.repository.DrinkRepository;
import com.symphonia.pairing.domain.repository.DrinkStyleRepository;
import com.symphonia.pairing.domain.vo.DrinkCategory;
import com.symphonia.pairing.domain.vo.DrinkSource;
import com.symphonia.pairing.domain.vo.DrinkStyleSource;
import com.symphonia.pairing.domain.vo.FlavorProfile;
import com.symphonia.pairing.domain.vo.WineAcidityRule;
import com.symphonia.pairing.domain.vo.WineAttribute;
import com.symphonia.pairing.domain.vo.WineBodyRule;
import com.symphonia.pairing.domain.vo.WineFlavorRule;
import com.symphonia.pairing.domain.vo.WineTypeRule;
import com.symphonia.pairing.domain.vo.XWine;
import com.symphonia.pairing.fixture.XWineFixture;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("ImportXWineService 단위 테스트")
class ImportXWineServiceTest extends UnitTest {

    private static final Long RED_STYLE_ID = 100L;
    private static final FlavorProfile RED_STYLE_PROFILE = FlavorProfile.of(1, 3, 0, 2, 2);

    // 스타일 기본 맛의 무게감(2)과 산미(2)가 제품의 Body, Acidity 값으로 덮어써지는지 보이도록 서로 다른 값을 둔다.
    private static final WineFlavorRule WINE_FLAVOR_RULE =
            new WineFlavorRule(
                    List.of(new WineTypeRule("Red", "red", "Red Wine", RED_STYLE_PROFILE)),
                    List.of(new WineBodyRule("Full-bodied", 4)),
                    List.of(new WineAcidityRule("Medium", 3)));

    @InjectMocks private ImportXWineService importXWineService;

    @Mock private XWineReader xWineReader;
    @Mock private WineFlavorRuleReader wineFlavorRuleReader;
    @Mock private DrinkStyleRepository drinkStyleRepository;
    @Mock private DrinkRepository drinkRepository;

    @Nested
    @DisplayName("importXWines 메서드는")
    class ImportXWines {

        @BeforeEach
        void setUp() {
            given(wineFlavorRuleReader.read()).willReturn(WINE_FLAVOR_RULE);
        }

        @Nested
        @DisplayName("처음 보는 와인 스타일인 경우")
        class WhenStyleNotImported {

            @Test
            @DisplayName("source는 X_WINES, 분류는 WINE으로 규칙표의 기본 맛을 담아 새로 저장한다")
            void shouldSaveNewWineStyle() {
                // given
                given(xWineReader.read()).willReturn(List.of());
                given(
                                drinkStyleRepository.findBySourceAndExternalId(
                                        DrinkStyleSource.X_WINES, "red"))
                        .willReturn(Optional.empty());
                given(drinkStyleRepository.save(any())).willReturn(savedRedStyle());

                // when
                ImportXWineResult result = importXWineService.importXWines();

                // then
                DrinkStyle saved = captureSavedStyle();
                assertThat(saved.getId()).isNull();
                assertThat(saved.getSource()).isEqualTo(DrinkStyleSource.X_WINES);
                assertThat(saved.getExternalId()).isEqualTo("red");
                assertThat(saved.getName()).isEqualTo("Red Wine");
                assertThat(saved.getCategory()).isEqualTo(DrinkCategory.WINE);
                assertThat(saved.getFlavorProfile())
                        .usingRecursiveComparison()
                        .isEqualTo(RED_STYLE_PROFILE);
                assertThat(result.importedStyleCount()).isEqualTo(1);
            }
        }

        @Nested
        @DisplayName("이미 적재된 와인 스타일인 경우")
        class WhenStyleAlreadyImported {

            @Test
            @DisplayName("id를 유지한 채 이름, 분류, 기본 맛을 갱신해 저장한다")
            void shouldUpdateExistingWineStyle() {
                // given
                DrinkStyle existing =
                        DrinkStyle.builder()
                                .id(RED_STYLE_ID)
                                .source(DrinkStyleSource.X_WINES)
                                .externalId("red")
                                .name("Old Red Wine")
                                .category(DrinkCategory.WINE)
                                .flavorProfile(FlavorProfile.of(5, 5, 5, 5, 5))
                                .build();
                given(xWineReader.read()).willReturn(List.of());
                given(
                                drinkStyleRepository.findBySourceAndExternalId(
                                        DrinkStyleSource.X_WINES, "red"))
                        .willReturn(Optional.of(existing));
                given(drinkStyleRepository.save(any())).willReturn(savedRedStyle());

                // when
                importXWineService.importXWines();

                // then
                DrinkStyle saved = captureSavedStyle();
                assertThat(saved.getId()).isEqualTo(RED_STYLE_ID);
                assertThat(saved.getName()).isEqualTo("Red Wine");
                assertThat(saved.getFlavorProfile())
                        .usingRecursiveComparison()
                        .isEqualTo(RED_STYLE_PROFILE);
            }
        }

        @Nested
        @DisplayName("처음 보는 와인인 경우")
        class WhenWineNotImported {

            @Test
            @DisplayName("스타일 id를 참조하고 와이너리명을 붙인 이름과 Body, Acidity로 덮어쓴 맛으로 새로 저장한다")
            void shouldSaveNewWineWithStyleAndOverriddenFlavor() {
                // given
                XWine wine = XWineFixture.ORIGEM_MERLOT.create();
                givenRedStyleSaved();
                given(xWineReader.read()).willReturn(List.of(wine));
                given(drinkRepository.findBySourceAndExternalId(DrinkSource.X_WINES, wine.wineId()))
                        .willReturn(Optional.empty());

                // when
                ImportXWineResult result = importXWineService.importXWines();

                // then
                Drink saved = captureSavedDrink();
                assertThat(saved.getId()).isNull();
                assertThat(saved.getDrinkStyleId()).isEqualTo(RED_STYLE_ID);
                assertThat(saved.getSource()).isEqualTo(DrinkSource.X_WINES);
                assertThat(saved.getExternalId()).isEqualTo(wine.wineId());
                assertThat(saved.getName()).isEqualTo("Casa Valduga Origem Merlot");
                assertThat(saved.getAbv()).isEqualTo(wine.abv());
                assertThat(saved.isNonAlcoholic()).isFalse();
                assertThat(saved.getFlavorProfile())
                        .usingRecursiveComparison()
                        .isEqualTo(FlavorProfile.of(1, 3, 0, 4, 3));
                assertThat(result.importedDrinkCount()).isEqualTo(1);
                assertThat(result.abvExceededItems()).isEmpty();
                assertThat(result.undeterminedItems()).isEmpty();
            }
        }

        @Nested
        @DisplayName("이미 적재된 와인인 경우")
        class WhenWineAlreadyImported {

            @Test
            @DisplayName("id를 유지한 채 스타일, 이름, 도수, 맛을 갱신해 저장한다")
            void shouldUpdateExistingWine() {
                // given
                XWine wine = XWineFixture.ORIGEM_MERLOT.createWithAbv(13.5);
                DrinkStyle redStyle = givenRedStyleSaved();
                Drink existing =
                        XWineFixture.ORIGEM_MERLOT.createImportedDrink(
                                10L, redStyle, FlavorProfile.of(5, 5, 5, 5, 5));
                given(xWineReader.read()).willReturn(List.of(wine));
                given(drinkRepository.findBySourceAndExternalId(DrinkSource.X_WINES, wine.wineId()))
                        .willReturn(Optional.of(existing));

                // when
                importXWineService.importXWines();

                // then
                Drink saved = captureSavedDrink();
                assertThat(saved.getId()).isEqualTo(10L);
                assertThat(saved.getAbv()).isEqualTo(13.5);
                assertThat(saved.getFlavorProfile())
                        .usingRecursiveComparison()
                        .isEqualTo(FlavorProfile.of(1, 3, 0, 4, 3));
            }
        }

        @Nested
        @DisplayName("도수가 상한(25%)을 넘는 경우")
        class WhenAbvExceedsLimit {

            @Test
            @DisplayName("저장하지 않고 abvExceededItems에 담는다")
            void shouldLeaveAbvExceededItemWithoutSaving() {
                // given
                XWine wine = XWineFixture.ORIGEM_MERLOT.createWithAbv(48.0);
                givenRedStyleSaved();
                given(xWineReader.read()).willReturn(List.of(wine));

                // when
                ImportXWineResult result = importXWineService.importXWines();

                // then
                verify(drinkRepository, never()).save(any());
                assertThat(result.importedDrinkCount()).isZero();
                assertThat(result.abvExceededItems())
                        .containsExactly(
                                new AbvExceededItem(
                                        wine.wineId(), "Casa Valduga Origem Merlot", 48.0));
            }

            @Test
            @DisplayName("상한과 같은 도수는 적재한다")
            void shouldSaveWineWhenAbvEqualsLimit() {
                // given
                XWine wine = XWineFixture.ORIGEM_MERLOT.createWithAbv(25.0);
                givenRedStyleSaved();
                given(xWineReader.read()).willReturn(List.of(wine));
                given(drinkRepository.findBySourceAndExternalId(DrinkSource.X_WINES, wine.wineId()))
                        .willReturn(Optional.empty());

                // when
                ImportXWineResult result = importXWineService.importXWines();

                // then
                assertThat(captureSavedDrink().getAbv()).isEqualTo(25.0);
                assertThat(result.abvExceededItems()).isEmpty();
            }
        }

        @Nested
        @DisplayName("규칙표에 없는 Type, Body, Acidity가 있는 경우")
        class WhenAttributesUnknown {

            @Test
            @DisplayName("저장하지 않고 규칙이 없는 속성을 undeterminedItems에 담는다")
            void shouldLeaveUndeterminedItemWithoutSaving() {
                // given
                XWine unknownEveryAttribute = XWineFixture.RESERVA_CHARDONNAY.create();
                XWine unknownBody = XWineFixture.ORIGEM_MERLOT.createWithBody("Watery");
                givenRedStyleSaved();
                given(xWineReader.read()).willReturn(List.of(unknownEveryAttribute, unknownBody));

                // when
                ImportXWineResult result = importXWineService.importXWines();

                // then
                verify(drinkRepository, never()).save(any());
                assertThat(result.importedDrinkCount()).isZero();
                assertThat(result.undeterminedItems())
                        .containsExactly(
                                new UndeterminedItem(
                                        unknownEveryAttribute.wineId(),
                                        "Aurora Reserva Chardonnay",
                                        Set.of(
                                                WineAttribute.TYPE,
                                                WineAttribute.BODY,
                                                WineAttribute.ACIDITY)),
                                new UndeterminedItem(
                                        unknownBody.wineId(),
                                        "Casa Valduga Origem Merlot",
                                        Set.of(WineAttribute.BODY)));
            }
        }
    }

    private DrinkStyle givenRedStyleSaved() {
        DrinkStyle saved = savedRedStyle();
        given(drinkStyleRepository.findBySourceAndExternalId(DrinkStyleSource.X_WINES, "red"))
                .willReturn(Optional.empty());
        given(drinkStyleRepository.save(any())).willReturn(saved);
        return saved;
    }

    private DrinkStyle savedRedStyle() {
        return DrinkStyle.builder()
                .id(RED_STYLE_ID)
                .source(DrinkStyleSource.X_WINES)
                .externalId("red")
                .name("Red Wine")
                .category(DrinkCategory.WINE)
                .flavorProfile(RED_STYLE_PROFILE)
                .build();
    }

    private DrinkStyle captureSavedStyle() {
        ArgumentCaptor<DrinkStyle> captor = ArgumentCaptor.forClass(DrinkStyle.class);
        verify(drinkStyleRepository).save(captor.capture());
        return captor.getValue();
    }

    private Drink captureSavedDrink() {
        ArgumentCaptor<Drink> captor = ArgumentCaptor.forClass(Drink.class);
        verify(drinkRepository).save(captor.capture());
        return captor.getValue();
    }
}
