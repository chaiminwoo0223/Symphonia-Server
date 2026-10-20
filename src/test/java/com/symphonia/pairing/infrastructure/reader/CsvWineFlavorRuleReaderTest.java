package com.symphonia.pairing.infrastructure.reader;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.pairing.domain.vo.Harmonize;
import com.symphonia.pairing.domain.vo.WineAcidityRule;
import com.symphonia.pairing.domain.vo.WineBodyRule;
import com.symphonia.pairing.domain.vo.WineEvaluation;
import com.symphonia.pairing.domain.vo.WineFlavorRule;
import com.symphonia.pairing.domain.vo.WineHarmonizeRule;
import com.symphonia.pairing.domain.vo.WineTypeRule;
import com.symphonia.pairing.infrastructure.reader.config.properties.XWinesProperties;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CsvWineFlavorRuleReader 단위 테스트")
class CsvWineFlavorRuleReaderTest {

    private final CsvWineFlavorRuleReader csvWineFlavorRuleReader = new CsvWineFlavorRuleReader();

    @Nested
    @DisplayName("read 메서드는")
    class Read {

        @Test
        @DisplayName("X-Wines의 Type 6종, Body 5단계, Acidity 3단계 규칙을 모두 읽는다")
        void shouldReadEveryXWinesValue() {
            // when
            WineFlavorRule rule = csvWineFlavorRuleReader.read();

            // then
            assertThat(rule.typeRules())
                    .extracting(WineTypeRule::type)
                    .containsExactly(
                            "Red", "White", "Rosé", "Sparkling", "Dessert", "Dessert/Port");
            assertThat(rule.bodyRules())
                    .extracting(WineBodyRule::body)
                    .containsExactly(
                            "Very light-bodied",
                            "Light-bodied",
                            "Medium-bodied",
                            "Full-bodied",
                            "Very full-bodied");
            assertThat(rule.acidityRules())
                    .extracting(WineAcidityRule::acidity)
                    .containsExactly("Low", "Medium", "High");
        }

        @Test
        @DisplayName("스타일 적재 키인 external_id와 유니크 제약이 걸린 이름이 중복되지 않는다")
        void shouldReadUniqueStyleKeys() {
            // when
            WineFlavorRule rule = csvWineFlavorRuleReader.read();

            // then
            assertThat(rule.typeRules())
                    .extracting(WineTypeRule::externalId)
                    .doesNotHaveDuplicates();
            assertThat(rule.typeRules()).extracting(WineTypeRule::name).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("Harmonize 상위 20개 태그는 같은 이름의 음식 계열 하나로 옮긴다")
        void shouldMapTopHarmonizeTagsToSameHarmonize() {
            // when
            WineFlavorRule rule = csvWineFlavorRuleReader.read();

            // then
            assertThat(rule.harmonizeRules().subList(0, Harmonize.values().length))
                    .extracting(WineHarmonizeRule::harmonizes)
                    .containsExactlyElementsOf(
                            Arrays.stream(Harmonize.values()).map(Set::of).toList());
        }

        @Test
        @DisplayName("맞는 음식 계열이 없는 태그는 비워 두고, 여러 계열에 걸치는 태그는 모두 담는다")
        void shouldReadEmptyAndMultipleHarmonizes() {
            // when
            WineFlavorRule rule = csvWineFlavorRuleReader.read();

            // then
            assertThat(harmonizesOf(rule, "Grilled")).isEmpty();
            assertThat(harmonizesOf(rule, "Seafood"))
                    .containsExactlyInAnyOrder(Harmonize.SHELLFISH, Harmonize.LEAN_FISH);
        }

        @Test
        @DisplayName("커밋된 X-Wines 원본의 모든 Type, Body, Acidity 값과 Harmonize 태그에 규칙이 있다")
        void shouldCoverEveryAttributeValueInCommittedSource() {
            // given
            CsvXWineReader csvXWineReader =
                    new CsvXWineReader(
                            new XWinesProperties(
                                    "scripts/import/data/x-wines/XWines_Slim_1K_wines.csv"));

            // when
            WineFlavorRule rule = csvWineFlavorRuleReader.read();

            // then
            assertThat(csvXWineReader.read())
                    .map(rule::evaluate)
                    .flatExtracting(WineEvaluation::unknownAttributes)
                    .isEmpty();
        }

        private Set<Harmonize> harmonizesOf(WineFlavorRule rule, String tag) {
            return rule.harmonizeRules().stream()
                    .filter(harmonizeRule -> harmonizeRule.matches(tag))
                    .findFirst()
                    .orElseThrow()
                    .harmonizes();
        }
    }
}
