package com.symphonia.catalog.infrastructure.reader;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.catalog.domain.vo.AllergyType;
import com.symphonia.catalog.domain.vo.BeerAllergyRule;
import com.symphonia.catalog.domain.vo.CuratedDrink;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CsvBeerAllergyRuleReader 단위 테스트")
class CsvBeerAllergyRuleReaderTest {

    private final CsvBeerAllergyRuleReader csvBeerAllergyRuleReader =
            new CsvBeerAllergyRuleReader();

    @Nested
    @DisplayName("read 메서드는")
    class Read {

        @Test
        @DisplayName("밀맥주 스타일에는 밀 알레르기를, 라거에는 빈 알레르기를 붙여 변환한다")
        void shouldMapColumnsToBeerAllergyRuleFields() {
            // when
            List<BeerAllergyRule> rules = csvBeerAllergyRuleReader.read();

            // then
            assertThat(rules)
                    .contains(
                            new BeerAllergyRule("10A", Set.of(AllergyType.WHEAT)),
                            new BeerAllergyRule("24A", Set.of(AllergyType.WHEAT)),
                            new BeerAllergyRule("2A", Set.of()));
        }

        @Test
        @DisplayName("BJCP 스타일 id가 중복되지 않는다")
        void shouldReadUniqueBjcpStyleIds() {
            // when
            List<BeerAllergyRule> rules = csvBeerAllergyRuleReader.read();

            // then
            assertThat(rules).extracting(BeerAllergyRule::bjcpStyleId).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("국내 유통 맥주 매핑표가 가리키는 모든 BJCP 스타일에 규칙이 있다")
        void shouldCoverEveryStyleInCuratedBeers() {
            // given
            List<CuratedDrink> curatedBeers = new CsvCuratedDrinkReader().read();

            // when
            List<BeerAllergyRule> rules = csvBeerAllergyRuleReader.read();

            // then
            assertThat(rules)
                    .extracting(BeerAllergyRule::bjcpStyleId)
                    .containsAll(curatedBeers.stream().map(CuratedDrink::bjcpStyleId).toList());
        }
    }
}
