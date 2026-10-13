package com.symphonia.pairing.infrastructure.reader;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.pairing.domain.vo.WineAcidityRule;
import com.symphonia.pairing.domain.vo.WineBodyRule;
import com.symphonia.pairing.domain.vo.WineEvaluation;
import com.symphonia.pairing.domain.vo.WineFlavorRule;
import com.symphonia.pairing.domain.vo.WineTypeRule;
import com.symphonia.pairing.infrastructure.reader.config.properties.XWinesProperties;
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
        @DisplayName("커밋된 X-Wines 원본의 모든 Type, Body, Acidity 값에 규칙이 있다")
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
    }
}
