package com.symphonia.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.symphonia.UnitTest;
import com.symphonia.catalog.application.dto.result.ImportXWineResult;
import com.symphonia.catalog.application.dto.result.ImportXWineResult.AbvExceededItem;
import com.symphonia.catalog.application.dto.result.ImportXWineResult.UndeterminedItem;
import com.symphonia.catalog.domain.entity.Drink;
import com.symphonia.catalog.domain.entity.DrinkStyle;
import com.symphonia.catalog.domain.reader.WineFlavorRuleReader;
import com.symphonia.catalog.domain.reader.XWineReader;
import com.symphonia.catalog.domain.repository.DrinkRepository;
import com.symphonia.catalog.domain.repository.DrinkStyleRepository;
import com.symphonia.catalog.domain.vo.DrinkCategory;
import com.symphonia.catalog.domain.vo.DrinkSource;
import com.symphonia.catalog.domain.vo.DrinkStyleSource;
import com.symphonia.catalog.domain.vo.FlavorProfile;
import com.symphonia.catalog.domain.vo.Harmonize;
import com.symphonia.catalog.domain.vo.WineAcidityRule;
import com.symphonia.catalog.domain.vo.WineAttribute;
import com.symphonia.catalog.domain.vo.WineBodyRule;
import com.symphonia.catalog.domain.vo.WineFlavorRule;
import com.symphonia.catalog.domain.vo.WineHarmonizeRule;
import com.symphonia.catalog.domain.vo.WineTypeRule;
import com.symphonia.catalog.domain.vo.XWine;
import com.symphonia.catalog.fixture.WineStyleFixture;
import com.symphonia.catalog.fixture.XWineFixture;
import java.util.List;
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
    private static final FlavorProfile RED_STYLE_PROFILE = WineStyleFixture.RED.getFlavorProfile();

    // 스타일 기본 맛의 무게감(2)과 산미(2)가 제품의 Body, Acidity 값으로 덮어써지는지 보이도록 서로 다른 값을 둔다.
    // ORIGEM_MERLOT의 Harmonize 태그 중 Grilled, Pizza는 맞는 계열이 없는 태그로 둔다.
    private static final WineFlavorRule WINE_FLAVOR_RULE =
            new WineFlavorRule(
                    List.of(new WineTypeRule("Red", "red", "Red Wine", RED_STYLE_PROFILE)),
                    List.of(new WineBodyRule("Full-bodied", 4)),
                    List.of(new WineAcidityRule("Medium", 3)),
                    List.of(
                            new WineHarmonizeRule("Beef", Set.of(Harmonize.BEEF)),
                            new WineHarmonizeRule("Lamb", Set.of(Harmonize.LAMB)),
                            new WineHarmonizeRule("Veal", Set.of(Harmonize.VEAL)),
                            new WineHarmonizeRule("Pasta", Set.of(Harmonize.PASTA)),
                            new WineHarmonizeRule("Grilled", Set.of()),
                            new WineHarmonizeRule("Pizza", Set.of())));

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
                given(drinkStyleRepository.findAllBySource(DrinkStyleSource.X_WINES))
                        .willReturn(List.of());
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
                DrinkStyle existing = WineStyleFixture.RED.createImportedStyle(RED_STYLE_ID);
                given(xWineReader.read()).willReturn(List.of());
                given(drinkStyleRepository.findAllBySource(DrinkStyleSource.X_WINES))
                        .willReturn(List.of(existing));
                given(drinkStyleRepository.save(any())).willReturn(savedRedStyle());

                // when
                importXWineService.importXWines();

                // then
                DrinkStyle saved = captureSavedStyle();
                assertThat(saved.getId()).isEqualTo(RED_STYLE_ID);
                assertThat(saved.getName()).isEqualTo(WineStyleFixture.RED.getName());
                assertThat(saved.getFlavorProfile())
                        .usingRecursiveComparison()
                        .isEqualTo(RED_STYLE_PROFILE);
            }
        }

        @Nested
        @DisplayName("처음 보는 와인인 경우")
        class WhenWineNotImported {

            @Test
            @DisplayName(
                    "스타일 id를 참조하고 와이너리명을 붙인 이름, Body와 Acidity로 덮어쓴 맛, Harmonize 태그의 음식 계열로 새로 저장한다")
            void shouldSaveNewWineWithStyleAndOverriddenFlavor() {
                // given
                XWine wine = XWineFixture.ORIGEM_MERLOT.create();
                givenRedStyleSaved();
                given(xWineReader.read()).willReturn(List.of(wine));
                given(drinkRepository.findAllBySource(DrinkSource.X_WINES)).willReturn(List.of());

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
                assertThat(saved.getHarmonizes())
                        .containsExactlyInAnyOrder(
                                Harmonize.BEEF, Harmonize.LAMB, Harmonize.VEAL, Harmonize.PASTA);
                assertThat(result.importedDrinkCount()).isEqualTo(1);
                assertThat(result.abvExceededItems()).isEmpty();
                assertThat(result.undeterminedItems()).isEmpty();
            }
        }

        @Nested
        @DisplayName("이미 적재된 와인인 경우")
        class WhenWineAlreadyImported {

            @Test
            @DisplayName("id를 유지한 채 스타일, 이름, 도수, 맛, 음식 계열을 갱신해 저장한다")
            void shouldUpdateExistingWine() {
                // given
                XWine wine = XWineFixture.ORIGEM_MERLOT.createWithAbv(13.5);
                DrinkStyle redStyle = givenRedStyleSaved();
                Drink existing =
                        XWineFixture.ORIGEM_MERLOT.createImportedDrink(
                                10L,
                                redStyle,
                                FlavorProfile.of(5, 5, 5, 5, 5),
                                Set.of(Harmonize.SWEET_DESSERT));
                given(xWineReader.read()).willReturn(List.of(wine));
                given(drinkRepository.findAllBySource(DrinkSource.X_WINES))
                        .willReturn(List.of(existing));

                // when
                importXWineService.importXWines();

                // then
                Drink saved = captureSavedDrink();
                assertThat(saved.getId()).isEqualTo(10L);
                assertThat(saved.getAbv()).isEqualTo(13.5);
                assertThat(saved.getFlavorProfile())
                        .usingRecursiveComparison()
                        .isEqualTo(FlavorProfile.of(1, 3, 0, 4, 3));
                assertThat(saved.getHarmonizes())
                        .containsExactlyInAnyOrder(
                                Harmonize.BEEF, Harmonize.LAMB, Harmonize.VEAL, Harmonize.PASTA);
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
        }

        @Nested
        @DisplayName("도수가 상한(25%)과 같은 경우")
        class WhenAbvEqualsLimit {

            @Test
            @DisplayName("상한을 넘지 않으므로 적재한다")
            void shouldSaveWine() {
                // given
                XWine wine = XWineFixture.ORIGEM_MERLOT.createWithAbv(25.0);
                givenRedStyleSaved();
                given(xWineReader.read()).willReturn(List.of(wine));
                given(drinkRepository.findAllBySource(DrinkSource.X_WINES)).willReturn(List.of());

                // when
                ImportXWineResult result = importXWineService.importXWines();

                // then
                assertThat(captureSavedDrink().getAbv()).isEqualTo(25.0);
                assertThat(result.abvExceededItems()).isEmpty();
            }
        }

        @Nested
        @DisplayName("규칙표에 없는 Type, Body, Acidity, Harmonize 태그가 있는 경우")
        class WhenAttributesUnknown {

            @Test
            @DisplayName("저장하지 않고 규칙이 없는 속성을 undeterminedItems에 담는다")
            void shouldLeaveUndeterminedItemWithoutSaving() {
                // given
                XWine unknownEveryAttribute = XWineFixture.RESERVA_CHARDONNAY.create();
                XWine unknownBody = XWineFixture.ORIGEM_MERLOT.createWithBody("Watery");
                XWine unknownHarmonizeTag =
                        XWineFixture.ORIGEM_MERLOT.createWithHarmonizeTags(
                                List.of("Beef", "Truffle"));
                givenRedStyleSaved();
                given(xWineReader.read())
                        .willReturn(
                                List.of(unknownEveryAttribute, unknownBody, unknownHarmonizeTag));

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
                                                WineAttribute.ACIDITY,
                                                WineAttribute.HARMONIZE)),
                                new UndeterminedItem(
                                        unknownBody.wineId(),
                                        "Casa Valduga Origem Merlot",
                                        Set.of(WineAttribute.BODY)),
                                new UndeterminedItem(
                                        unknownHarmonizeTag.wineId(),
                                        "Casa Valduga Origem Merlot",
                                        Set.of(WineAttribute.HARMONIZE)));
            }
        }
    }

    private DrinkStyle givenRedStyleSaved() {
        DrinkStyle saved = savedRedStyle();
        given(drinkStyleRepository.findAllBySource(DrinkStyleSource.X_WINES)).willReturn(List.of());
        given(drinkStyleRepository.save(any())).willReturn(saved);
        return saved;
    }

    private DrinkStyle savedRedStyle() {
        return WineStyleFixture.RED.createWithId(RED_STYLE_ID);
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
