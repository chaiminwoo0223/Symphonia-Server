package com.symphonia.pairing.infrastructure.reader;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.pairing.domain.vo.BeerHarmonizeRule;
import com.symphonia.pairing.domain.vo.CuratedDrink;
import com.symphonia.pairing.domain.vo.Harmonize;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CsvBeerHarmonizeRuleReader 단위 테스트")
class CsvBeerHarmonizeRuleReaderTest {

    private final CsvBeerHarmonizeRuleReader csvBeerHarmonizeRuleReader =
            new CsvBeerHarmonizeRuleReader();

    @Nested
    @DisplayName("read 메서드는")
    class Read {

        @Test
        @DisplayName("BJCP 스타일 id와 |로 나눈 음식 계열을 BeerHarmonizeRule로 변환한다")
        void shouldMapColumnsToBeerHarmonizeRuleFields() {
            // when
            List<BeerHarmonizeRule> rules = csvBeerHarmonizeRuleReader.read();

            // then
            assertThat(rules)
                    .contains(
                            new BeerHarmonizeRule(
                                    "2A",
                                    Set.of(
                                            Harmonize.POULTRY,
                                            Harmonize.VEGETARIAN,
                                            Harmonize.RICH_FISH,
                                            Harmonize.CURED_MEAT)),
                            new BeerHarmonizeRule(
                                    "15B", Set.of(Harmonize.BEEF, Harmonize.SHELLFISH)));
        }

        @Test
        @DisplayName("BJCP 스타일 id가 중복되지 않는다")
        void shouldReadUniqueBjcpStyleIds() {
            // when
            List<BeerHarmonizeRule> rules = csvBeerHarmonizeRuleReader.read();

            // then
            assertThat(rules).extracting(BeerHarmonizeRule::bjcpStyleId).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("국내 유통 맥주 매핑표가 가리키는 모든 BJCP 스타일에 규칙이 있다")
        void shouldCoverEveryStyleInCuratedBeers() {
            // given
            List<CuratedDrink> curatedBeers = new CsvCuratedDrinkReader().read();

            // when
            List<BeerHarmonizeRule> rules = csvBeerHarmonizeRuleReader.read();

            // then
            assertThat(rules)
                    .extracting(BeerHarmonizeRule::bjcpStyleId)
                    .containsAll(curatedBeers.stream().map(CuratedDrink::bjcpStyleId).toList());
        }
    }
}
