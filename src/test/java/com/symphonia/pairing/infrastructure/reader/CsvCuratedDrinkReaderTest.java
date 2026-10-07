package com.symphonia.pairing.infrastructure.reader;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.pairing.domain.vo.CuratedDrink;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CsvCuratedDrinkReader 단위 테스트")
class CsvCuratedDrinkReaderTest {

    private final CsvCuratedDrinkReader csvCuratedDrinkReader = new CsvCuratedDrinkReader();

    @Nested
    @DisplayName("read 메서드는")
    class Read {

        @Test
        @DisplayName("클래스패스의 매핑표를 모든 행 그대로 읽는다")
        void shouldReadRowCountOfClasspathMappingTable() {
            // when
            List<CuratedDrink> curatedDrinks = csvCuratedDrinkReader.read();

            // then
            assertThat(curatedDrinks).hasSize(21);
        }

        @Test
        @DisplayName("적재 키인 external_id가 중복되지 않는다")
        void shouldReadUniqueExternalIds() {
            // when
            List<CuratedDrink> curatedDrinks = csvCuratedDrinkReader.read();

            // then
            assertThat(curatedDrinks).extracting(CuratedDrink::externalId).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("각 열을 CuratedDrink의 필드로 변환한다")
        void shouldMapColumnsToCuratedDrinkFields() {
            // when
            List<CuratedDrink> curatedDrinks = csvCuratedDrinkReader.read();

            // then
            assertThat(curatedDrinks)
                    .first()
                    .isEqualTo(new CuratedDrink("cass-fresh", "카스 프레시", 4.5, "2A"));
            assertThat(curatedDrinks)
                    .contains(
                            new CuratedDrink("guinness-draught", "기네스 드래프트", 4.2, "15B"),
                            new CuratedDrink("hoegaarden", "호가든", 4.9, "24A"));
        }
    }
}
