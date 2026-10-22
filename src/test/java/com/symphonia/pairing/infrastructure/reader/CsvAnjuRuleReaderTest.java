package com.symphonia.pairing.infrastructure.reader;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.pairing.domain.vo.AllergyType;
import com.symphonia.pairing.domain.vo.AnjuRule;
import com.symphonia.pairing.domain.vo.IngredientRule;
import com.symphonia.pairing.domain.vo.NutrientRule;
import com.symphonia.pairing.domain.vo.ServingRichnessRule;
import com.symphonia.pairing.domain.vo.ServingStyle;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CsvAnjuRuleReader 단위 테스트")
class CsvAnjuRuleReaderTest {

    private final CsvAnjuRuleReader csvAnjuRuleReader = new CsvAnjuRuleReader();

    @Nested
    @DisplayName("read 메서드는")
    class Read {

        @Test
        @DisplayName("당류 구간을 빈틈없이 이어진 1~5 단맛으로 읽는다")
        void shouldReadContiguousSugarRules() {
            // when
            AnjuRule anjuRule = csvAnjuRuleReader.read();

            // then
            assertContiguousFromZero(anjuRule.sugarRules());
        }

        @Test
        @DisplayName("지방 구간을 빈틈없이 이어진 1~5 무게감으로 읽는다")
        void shouldReadContiguousFatRules() {
            // when
            AnjuRule anjuRule = csvAnjuRuleReader.read();

            // then
            assertContiguousFromZero(anjuRule.fatRules());
        }

        @Test
        @DisplayName("튀기지 않은 집어 먹는 안주만 무게감을 1 낮추는 형태 보정 규칙으로 읽는다")
        void shouldReadServingRichnessRules() {
            // when
            AnjuRule anjuRule = csvAnjuRuleReader.read();

            // then
            assertThat(anjuRule.servingRichnessRules())
                    .containsExactly(new ServingRichnessRule(ServingStyle.SNACK, false, -1));
        }

        @Test
        @DisplayName("재료 키워드가 중복되지 않는다")
        void shouldReadUniqueIngredientKeywords() {
            // when
            AnjuRule anjuRule = csvAnjuRuleReader.read();

            // then
            assertThat(anjuRule.ingredientRules())
                    .extracting(IngredientRule::keyword)
                    .doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("|로 나눈 알레르기와 매운맛, 산미, 쓴맛을 IngredientRule의 필드로 변환한다")
        void shouldMapColumnsToIngredientRuleFields() {
            // when
            AnjuRule anjuRule = csvAnjuRuleReader.read();

            // then
            assertThat(anjuRule.ingredientRules())
                    .contains(
                            new IngredientRule(
                                    "고추장",
                                    Set.of(AllergyType.SOYBEAN, AllergyType.WHEAT),
                                    true,
                                    0,
                                    0),
                            new IngredientRule("식초", Set.of(), false, 4, 0));
        }

        @Test
        @DisplayName("매핑표의 모든 재료에 맞는 규칙이 있어 규칙표에 없는 재료로 제외되는 안주가 없다")
        void shouldCoverIngredientsOfMappingTable() {
            // given
            List<String> ingredients =
                    new CsvCuratedAnjuReader()
                            .read().stream()
                                    .flatMap(curatedAnju -> curatedAnju.ingredients().stream())
                                    .distinct()
                                    .toList();

            // when
            AnjuRule anjuRule = csvAnjuRuleReader.read();

            // then
            assertThat(ingredients)
                    .allSatisfy(
                            ingredient ->
                                    assertThat(anjuRule.ingredientRules())
                                            .as("규칙표에 없는 재료: %s", ingredient)
                                            .anyMatch(rule -> rule.matches(ingredient)));
        }
    }

    private void assertContiguousFromZero(List<NutrientRule> rules) {
        assertThat(rules).first().extracting(NutrientRule::minGrams).isEqualTo(0.0);
        assertThat(rules).extracting(NutrientRule::value).containsExactly(1, 2, 3, 4, 5);
        assertThat(IntStream.range(1, rules.size()))
                .allSatisfy(
                        index ->
                                assertThat(rules.get(index).minGrams())
                                        .as("%d번째 구간의 시작", index)
                                        .isEqualTo(rules.get(index - 1).maxGrams()));
    }
}
