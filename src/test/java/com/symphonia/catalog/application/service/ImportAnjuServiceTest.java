package com.symphonia.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.symphonia.UnitTest;
import com.symphonia.catalog.application.dto.result.ImportAnjuResult;
import com.symphonia.catalog.application.dto.result.ImportAnjuResult.ExcludedAnju;
import com.symphonia.catalog.application.dto.result.ImportAnjuResult.RemovedAnju;
import com.symphonia.catalog.domain.entity.Anju;
import com.symphonia.catalog.domain.reader.AnjuNutritionReader;
import com.symphonia.catalog.domain.reader.AnjuRuleReader;
import com.symphonia.catalog.domain.reader.CuratedAnjuReader;
import com.symphonia.catalog.domain.repository.AnjuRepository;
import com.symphonia.catalog.domain.vo.AllergyType;
import com.symphonia.catalog.domain.vo.AnjuExclusionReason;
import com.symphonia.catalog.domain.vo.AnjuNutrition;
import com.symphonia.catalog.domain.vo.AnjuSource;
import com.symphonia.catalog.domain.vo.CuratedAnju;
import com.symphonia.catalog.domain.vo.FlavorProfile;
import com.symphonia.catalog.domain.vo.Harmonize;
import com.symphonia.catalog.fixture.AnjuRuleFixture;
import com.symphonia.catalog.fixture.CuratedAnjuFixture;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("ImportAnjuService 단위 테스트")
class ImportAnjuServiceTest extends UnitTest {

    @InjectMocks private ImportAnjuService importAnjuService;

    @Mock private CuratedAnjuReader curatedAnjuReader;
    @Mock private AnjuNutritionReader anjuNutritionReader;
    @Mock private AnjuRuleReader anjuRuleReader;
    @Mock private AnjuRepository anjuRepository;

    @BeforeEach
    void setUp() {
        given(anjuRuleReader.read()).willReturn(AnjuRuleFixture.STANDARD.create());
    }

    @Nested
    @DisplayName("importCuratedAnjus 메서드는")
    class ImportCuratedAnjus {

        @Nested
        @DisplayName("처음 보는 안주인 경우")
        class WhenAnjuNotImported {

            @Test
            @DisplayName("당류와 지방 구간으로 단맛과 무게감을, 재료 규칙으로 알레르기와 매운맛을 정해 새로 저장한다")
            void shouldSaveNewAnjuWithCalculatedFlavorAndAllergies() {
                // given
                CuratedAnjuFixture fixture = CuratedAnjuFixture.DAKBAL_BOKKEUM;
                givenCuratedAnjus(List.of(fixture.create()), List.of(fixture.createNutrition()));
                givenNotImported();

                // when
                ImportAnjuResult result = importAnjuService.importCuratedAnjus();

                // then
                Anju saved = captureSavedAnju();
                assertThat(saved.getId()).isNull();
                assertThat(saved.getSource()).isEqualTo(AnjuSource.K_FIND);
                assertThat(saved.getExternalId()).isEqualTo(fixture.getExternalId());
                assertThat(saved.getName()).isEqualTo(fixture.getName());
                assertThat(saved.getFlavorProfile())
                        .usingRecursiveComparison()
                        .isEqualTo(FlavorProfile.of(1, 0, 0, 4, 0));
                assertThat(saved.getAllergyTypes())
                        .containsExactlyInAnyOrder(
                                AllergyType.CHICKEN, AllergyType.SOYBEAN, AllergyType.WHEAT);
                assertThat(saved.isSpicy()).isTrue();
                assertThat(saved.getBase()).isEqualTo(fixture.getBase());
                assertThat(saved.getServing()).isEqualTo(fixture.getServing());
                assertThat(result.importedCount()).isEqualTo(1);
                assertThat(result.excludedAnjus()).isEmpty();
            }

            @Test
            @DisplayName("부재료의 알레르기는 넣되, 매운 재료가 부재료에만 있으면 매운 안주로 보지 않는다")
            void shouldNotMarkSpicyWhenSpicyIngredientOnlyInSubIngredients() {
                // given
                CuratedAnjuFixture fixture = CuratedAnjuFixture.FRIED_CHICKEN;
                givenCuratedAnjus(List.of(fixture.create()), List.of(fixture.createNutrition()));
                givenNotImported();

                // when
                importAnjuService.importCuratedAnjus();

                // then
                Anju saved = captureSavedAnju();
                assertThat(saved.isSpicy()).isFalse();
                assertThat(saved.getAllergyTypes())
                        .containsExactlyInAnyOrder(
                                AllergyType.CHICKEN, AllergyType.WHEAT, AllergyType.EGG);
            }

            @Test
            @DisplayName("산미를 내는 재료가 부재료에만 있어도 산미에 반영한다")
            void shouldReflectAcidityOfSubIngredients() {
                // given
                CuratedAnjuFixture fixture = CuratedAnjuFixture.GOLBAENGI_MUCHIM;
                givenCuratedAnjus(List.of(fixture.create()), List.of(fixture.createNutrition()));
                givenNotImported();

                // when
                importAnjuService.importCuratedAnjus();

                // then
                assertThat(captureSavedAnju().getFlavorProfile())
                        .usingRecursiveComparison()
                        .isEqualTo(FlavorProfile.of(4, 0, 0, 1, 4));
            }

            @Test
            @DisplayName("집어 먹는 안주면 지방 구간의 무게감을 형태 규칙만큼 낮춘다")
            void shouldLowerRichnessWhenAnjuIsSnack() {
                // given
                CuratedAnjuFixture fixture = CuratedAnjuFixture.BEEF_JERKY;
                givenCuratedAnjus(
                        List.of(fixture.create()),
                        List.of(fixture.createNutrition(fixture.getSugarGrams(), 15.0)));
                givenNotImported();

                // when
                importAnjuService.importCuratedAnjus();

                // then
                assertThat(captureSavedAnju().getFlavorProfile())
                        .usingRecursiveComparison()
                        .isEqualTo(FlavorProfile.of(4, 0, 0, 3, 0));
            }

            @Test
            @DisplayName("집어 먹는 안주라도 튀긴 요리면 지방 구간의 무게감을 그대로 쓴다")
            void shouldKeepRichnessWhenSnackIsFried() {
                // given
                CuratedAnjuFixture fixture = CuratedAnjuFixture.CHICKEN_NUGGET;
                givenCuratedAnjus(List.of(fixture.create()), List.of(fixture.createNutrition()));
                givenNotImported();

                // when
                importAnjuService.importCuratedAnjus();

                // then
                assertThat(captureSavedAnju().getFlavorProfile())
                        .usingRecursiveComparison()
                        .isEqualTo(FlavorProfile.of(1, 0, 0, 4, 0));
            }

            @Test
            @DisplayName("집어 먹는 안주라도 무게감을 가장 낮은 구간보다 낮추지 않는다")
            void shouldKeepLowestRichnessWhenSnackIsAlreadyLightest() {
                // given
                CuratedAnjuFixture fixture = CuratedAnjuFixture.BEEF_JERKY;
                givenCuratedAnjus(List.of(fixture.create()), List.of(fixture.createNutrition()));
                givenNotImported();

                // when
                importAnjuService.importCuratedAnjus();

                // then
                assertThat(captureSavedAnju().getFlavorProfile())
                        .usingRecursiveComparison()
                        .isEqualTo(FlavorProfile.of(4, 0, 0, 1, 0));
            }

            @Test
            @DisplayName("가공식품이면 원재료명에서 찾은 알레르기도 함께 넣는다")
            void shouldIncludeAllergiesOfRawMaterials() {
                // given
                CuratedAnjuFixture fixture = CuratedAnjuFixture.BEEF_JERKY;
                givenCuratedAnjus(List.of(fixture.create()), List.of(fixture.createNutrition()));
                givenNotImported();

                // when
                importAnjuService.importCuratedAnjus();

                // then
                assertThat(captureSavedAnju().getAllergyTypes())
                        .containsExactlyInAnyOrder(
                                AllergyType.BEEF,
                                AllergyType.SOYBEAN,
                                AllergyType.WHEAT,
                                AllergyType.PEANUT);
            }
        }

        @Nested
        @DisplayName("이미 적재된 안주인 경우")
        class WhenAnjuAlreadyImported {

            @Test
            @DisplayName("id를 유지한 채 이름, 맛, 알레르기, 분류를 갱신해 저장한다")
            void shouldUpdateExistingAnju() {
                // given
                CuratedAnjuFixture fixture = CuratedAnjuFixture.FRIED_CHICKEN;
                givenCuratedAnjus(List.of(fixture.create()), List.of(fixture.createNutrition()));
                given(anjuRepository.findAll()).willReturn(List.of(fixture.createImportedAnju(2L)));
                givenSaveReturnsArgument();

                // when
                ImportAnjuResult result = importAnjuService.importCuratedAnjus();

                // then
                Anju saved = captureSavedAnju();
                assertThat(saved.getId()).isEqualTo(2L);
                assertThat(saved.getSource()).isEqualTo(AnjuSource.SEED);
                assertThat(saved.getExternalId()).isEqualTo(fixture.getExternalId());
                assertThat(saved.getName()).isEqualTo(fixture.getName());
                assertThat(saved.getFlavorProfile())
                        .usingRecursiveComparison()
                        .isEqualTo(FlavorProfile.of(1, 0, 0, 4, 0));
                assertThat(saved.getAllergyTypes())
                        .containsExactlyInAnyOrder(
                                AllergyType.CHICKEN, AllergyType.WHEAT, AllergyType.EGG);
                assertThat(saved.getBase()).isEqualTo(fixture.getBase());
                assertThat(saved.getServing()).isEqualTo(fixture.getServing());
                assertThat(saved.isSpicy()).isFalse();
                assertThat(result.importedCount()).isEqualTo(1);
            }
        }

        @Nested
        @DisplayName("external_id가 같지만 출처가 다른 안주만 적재된 경우")
        class WhenSameExternalIdWithDifferentSourceImported {

            @Test
            @DisplayName("다른 안주로 보고 기존 안주를 덮어쓰지 않은 채 새로 저장한다")
            void shouldSaveAsNewAnju() {
                // given
                CuratedAnjuFixture fixture = CuratedAnjuFixture.DAKBAL_BOKKEUM;
                Anju sameKeyOfSeed = fixture.createImportedAnju(3L, AnjuSource.SEED);
                givenCuratedAnjus(List.of(fixture.create()), List.of(fixture.createNutrition()));
                given(anjuRepository.findAll()).willReturn(List.of(sameKeyOfSeed));
                givenSaveReturnsArgument();

                // when
                importAnjuService.importCuratedAnjus();

                // then
                Anju saved = captureSavedAnju();
                assertThat(saved.getId()).isNull();
                assertThat(saved.getSource()).isEqualTo(AnjuSource.K_FIND);
                assertThat(sameKeyOfSeed)
                        .usingRecursiveComparison()
                        .isEqualTo(fixture.createImportedAnju(3L, AnjuSource.SEED));
            }
        }

        @Nested
        @DisplayName("이미 적재됐지만 이번에 적재하지 않은 안주가 있는 경우")
        class WhenStaleAnjusExist {

            @Test
            @DisplayName("K_FIND 안주는 피드백이 참조하지 않으면 지우고, 참조하면 남기며, 시드 안주는 지우지 않는다")
            void shouldDeleteUnreferencedStaleKfindAnjus() {
                // given
                CuratedAnjuFixture fixture = CuratedAnjuFixture.DAKBAL_BOKKEUM;
                Anju unreferenced = CuratedAnjuFixture.BEEF_JERKY.createImportedAnju(7L);
                Anju referenced = CuratedAnjuFixture.GOLBAENGI_MUCHIM.createImportedAnju(8L);
                Anju seed = CuratedAnjuFixture.FRIED_CHICKEN.createImportedAnju(2L);
                givenCuratedAnjus(List.of(fixture.create()), List.of(fixture.createNutrition()));
                given(anjuRepository.findAll()).willReturn(List.of(unreferenced, referenced, seed));
                given(anjuRepository.findReferencedIds(any())).willReturn(Set.of(8L));
                givenSaveReturnsArgument();

                // when
                ImportAnjuResult result = importAnjuService.importCuratedAnjus();

                // then
                verify(anjuRepository).deleteAllById(List.of(7L));
                assertThat(result.deletedAnjus())
                        .containsExactly(
                                new RemovedAnju(
                                        unreferenced.getExternalId(), unreferenced.getName()));
                assertThat(result.retainedAnjus())
                        .containsExactly(
                                new RemovedAnju(referenced.getExternalId(), referenced.getName()));
            }

            @Test
            @DisplayName("같은 이름의 안주를 저장할 수 있도록 지우기를 저장보다 먼저 한다")
            void shouldDeleteBeforeSaving() {
                // given
                CuratedAnjuFixture fixture = CuratedAnjuFixture.DAKBAL_BOKKEUM;
                givenCuratedAnjus(List.of(fixture.create()), List.of(fixture.createNutrition()));
                given(anjuRepository.findAll())
                        .willReturn(List.of(CuratedAnjuFixture.BEEF_JERKY.createImportedAnju(7L)));
                givenSaveReturnsArgument();

                // when
                importAnjuService.importCuratedAnjus();

                // then
                InOrder inOrder = inOrder(anjuRepository);
                inOrder.verify(anjuRepository).deleteAllById(List.of(7L));
                inOrder.verify(anjuRepository).save(any());
            }
        }

        @Nested
        @DisplayName("식별 키와 식품코드를 채우지 않은 경우")
        class WhenIdentityMissing {

            @Test
            @DisplayName("저장하지 않고 두 사유를 함께 담는다")
            void shouldExcludeWithMissingIdentityReasons() {
                // given
                CuratedAnju curatedAnju =
                        CuratedAnjuFixture.DAKBAL_BOKKEUM.createWithIdentity("", "");
                givenCuratedAnjus(List.of(curatedAnju), List.of());

                // when
                ImportAnjuResult result = importAnjuService.importCuratedAnjus();

                // then
                verify(anjuRepository, never()).save(any());
                assertThat(result.importedCount()).isZero();
                assertThat(result.excludedAnjus())
                        .singleElement()
                        .extracting(ExcludedAnju::reasons)
                        .isEqualTo(
                                Set.of(
                                        AnjuExclusionReason.MISSING_EXTERNAL_ID,
                                        AnjuExclusionReason.MISSING_KFIND_CODE));
            }
        }

        @Nested
        @DisplayName("식품코드에 맞는 영양성분이 없는 경우")
        class WhenNutritionNotFound {

            @Test
            @DisplayName("저장하지 않고 NUTRITION_NOT_FOUND 사유를 담는다")
            void shouldExcludeWithNutritionNotFound() {
                // given
                givenCuratedAnjus(List.of(CuratedAnjuFixture.DAKBAL_BOKKEUM.create()), List.of());

                // when
                ImportAnjuResult result = importAnjuService.importCuratedAnjus();

                // then
                verify(anjuRepository, never()).save(any());
                assertThat(result.excludedAnjus())
                        .singleElement()
                        .extracting(ExcludedAnju::reasons)
                        .isEqualTo(Set.of(AnjuExclusionReason.NUTRITION_NOT_FOUND));
            }
        }

        @Nested
        @DisplayName("K-FIND가 당류와 지방을 측정하지 않은 경우")
        class WhenNutrientsNotMeasured {

            @Test
            @DisplayName("저장하지 않고 측정되지 않은 성분마다 사유를 담는다")
            void shouldExcludeWithNotMeasuredReasons() {
                // given
                CuratedAnjuFixture fixture = CuratedAnjuFixture.DAKBAL_BOKKEUM;
                givenCuratedAnjus(
                        List.of(fixture.create()), List.of(fixture.createNutrition(null, null)));

                // when
                ImportAnjuResult result = importAnjuService.importCuratedAnjus();

                // then
                verify(anjuRepository, never()).save(any());
                assertThat(result.excludedAnjus())
                        .singleElement()
                        .extracting(ExcludedAnju::reasons)
                        .isEqualTo(
                                Set.of(
                                        AnjuExclusionReason.SUGAR_NOT_MEASURED,
                                        AnjuExclusionReason.FAT_NOT_MEASURED));
            }
        }

        @Nested
        @DisplayName("규칙표에 없는 재료가 있는 경우")
        class WhenIngredientUnknown {

            @Test
            @DisplayName("알레르기를 놓치지 않도록 저장하지 않고 그 재료를 결과에 담는다")
            void shouldExcludeWithUnknownIngredients() {
                // given
                CuratedAnjuFixture fixture = CuratedAnjuFixture.DAKBAL_BOKKEUM;
                CuratedAnju curatedAnju = fixture.createWithSubIngredients(List.of("청경채", "마늘"));
                givenCuratedAnjus(List.of(curatedAnju), List.of(fixture.createNutrition()));

                // when
                ImportAnjuResult result = importAnjuService.importCuratedAnjus();

                // then
                verify(anjuRepository, never()).save(any());
                assertThat(result.excludedAnjus())
                        .singleElement()
                        .satisfies(
                                anju -> {
                                    assertThat(anju.reasons())
                                            .containsExactly(
                                                    AnjuExclusionReason.UNKNOWN_INGREDIENT);
                                    assertThat(anju.unknownIngredients()).containsExactly("청경채");
                                });
                assertThat(result.unknownIngredients()).containsExactly("청경채");
            }
        }

        @Nested
        @DisplayName("적재한 안주가 여러 개인 경우")
        class WhenAnjusImported {

            @Test
            @DisplayName("주재료 계열, 형태, 매운맛 여부로 Harmonize 태그별 안주 수를 세고 맞는 안주가 없는 태그는 0으로 둔다")
            void shouldCountAnjusPerHarmonize() {
                // given
                CuratedAnjuFixture dakbal = CuratedAnjuFixture.DAKBAL_BOKKEUM;
                CuratedAnjuFixture beefJerky = CuratedAnjuFixture.BEEF_JERKY;
                givenCuratedAnjus(
                        List.of(dakbal.create(), beefJerky.create()),
                        List.of(dakbal.createNutrition(), beefJerky.createNutrition()));
                givenNotImported();

                // when
                ImportAnjuResult result = importAnjuService.importCuratedAnjus();

                // then
                assertThat(result.harmonizeCounts()).hasSize(Harmonize.values().length);
                assertThat(result.harmonizeCounts())
                        .containsEntry(Harmonize.POULTRY, 1)
                        .containsEntry(Harmonize.SPICY_FOOD, 1)
                        .containsEntry(Harmonize.BEEF, 1)
                        .containsEntry(Harmonize.VEAL, 1)
                        .containsEntry(Harmonize.SNACK, 1)
                        .containsEntry(Harmonize.PORK, 0)
                        .containsEntry(Harmonize.APPETIZER, 0);
            }
        }
    }

    private void givenCuratedAnjus(List<CuratedAnju> curatedAnjus, List<AnjuNutrition> nutritions) {
        given(curatedAnjuReader.read()).willReturn(curatedAnjus);
        given(anjuNutritionReader.read()).willReturn(nutritions);
    }

    private void givenNotImported() {
        given(anjuRepository.findAll()).willReturn(List.of());
        givenSaveReturnsArgument();
    }

    private void givenSaveReturnsArgument() {
        given(anjuRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
    }

    private Anju captureSavedAnju() {
        ArgumentCaptor<Anju> captor = ArgumentCaptor.forClass(Anju.class);
        verify(anjuRepository).save(captor.capture());
        return captor.getValue();
    }
}
