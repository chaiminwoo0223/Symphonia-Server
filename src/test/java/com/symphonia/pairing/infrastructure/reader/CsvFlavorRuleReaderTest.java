package com.symphonia.pairing.infrastructure.reader;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.pairing.domain.vo.FlavorAxis;
import com.symphonia.pairing.domain.vo.FlavorRule;
import com.symphonia.pairing.domain.vo.IbuRule;
import com.symphonia.pairing.domain.vo.MouthfeelRule;
import com.symphonia.pairing.domain.vo.TagRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CsvFlavorRuleReader 단위 테스트")
class CsvFlavorRuleReaderTest {

    private final CsvFlavorRuleReader csvFlavorRuleReader = new CsvFlavorRuleReader();

    @Nested
    @DisplayName("read 메서드는")
    class Read {

        @Test
        @DisplayName("클래스패스의 세 규칙표를 모든 행 그대로 읽는다")
        void shouldReadRuleCountsOfClasspathRuleTables() {
            // when
            FlavorRule flavorRule = csvFlavorRuleReader.read();

            // then
            assertThat(flavorRule.ibuRules()).hasSize(5);
            assertThat(flavorRule.tagRules()).hasSize(8);
            assertThat(flavorRule.mouthfeelRules()).hasSize(54);
        }

        @Test
        @DisplayName("각 열을 규칙의 필드로 변환한다")
        void shouldMapColumnsToRuleFields() {
            // when
            FlavorRule flavorRule = csvFlavorRuleReader.read();

            // then
            assertThat(flavorRule.ibuRules()).first().isEqualTo(new IbuRule(0, 10, 1));
            assertThat(flavorRule.tagRules()).contains(new TagRule("sour", FlavorAxis.ACIDITY, 4));
            assertThat(flavorRule.mouthfeelRules())
                    .contains(new MouthfeelRule("low carbonation", FlavorAxis.CARBONATION, 1));
        }

        @Test
        @DisplayName("따옴표 안에 쉼표가 든 키워드를 한 필드로 읽는다")
        void shouldReadQuotedKeywordWithCommasAsSingleField() {
            // when
            FlavorRule flavorRule = csvFlavorRuleReader.read();

            // then
            assertThat(flavorRule.mouthfeelRules())
                    .contains(
                            new MouthfeelRule(
                                    "very light, sometimes watery, body", FlavorAxis.RICHNESS, 0),
                            new MouthfeelRule(
                                    "light and crisp, although body can reach medium",
                                    FlavorAxis.RICHNESS,
                                    2),
                            new MouthfeelRule(
                                    "medium to full, chewy body", FlavorAxis.RICHNESS, 4));
        }
    }
}
