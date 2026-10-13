package com.symphonia.pairing.infrastructure.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.symphonia.pairing.domain.exception.DrinkImportSourceReadFailedException;
import com.symphonia.pairing.domain.vo.XWine;
import com.symphonia.pairing.infrastructure.reader.config.properties.XWinesProperties;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CsvXWineReader 단위 테스트")
class CsvXWineReaderTest {

    @Nested
    @DisplayName("read 메서드는")
    class Read {

        @Nested
        @DisplayName("파일이 있는 경우")
        class WhenFileExists {

            private final CsvXWineReader csvXWineReader =
                    new CsvXWineReader(
                            new XWinesProperties("src/test/resources/import/x-wines.csv"));

            @Test
            @DisplayName("모든 와인을 파일 순서대로 읽는다")
            void shouldReadWinesInFileOrder() {
                // when
                List<XWine> wines = csvXWineReader.read();

                // then
                assertThat(wines)
                        .extracting(XWine::type)
                        .containsExactly("Red", "Rosé", "Dessert/Port");
            }

            @Test
            @DisplayName("따옴표 안에 쉼표가 있는 열이 있어도 각 열을 XWine의 필드로 변환한다")
            void shouldMapColumnsToXWineFields() {
                // when
                List<XWine> wines = csvXWineReader.read();

                // then
                assertThat(wines)
                        .first()
                        .isEqualTo(
                                new XWine(
                                        "100062",
                                        "Origem Merlot",
                                        "Casa Valduga",
                                        "Red",
                                        13.0,
                                        "Full-bodied",
                                        "Medium"));
            }
        }

        @Nested
        @DisplayName("레포에 커밋된 Slim 원본인 경우")
        class WhenCommittedSlimSource {

            private final CsvXWineReader csvXWineReader =
                    new CsvXWineReader(
                            new XWinesProperties(
                                    "scripts/import/data/x-wines/XWines_Slim_1K_wines.csv"));

            @Test
            @DisplayName("1,007종을 모두 읽고 적재 키인 WineID가 중복되지 않는다")
            void shouldReadEveryWineWithUniqueWineIds() {
                // when
                List<XWine> wines = csvXWineReader.read();

                // then
                assertThat(wines).hasSize(1007);
                assertThat(wines).extracting(XWine::wineId).doesNotHaveDuplicates();
            }
        }

        @Nested
        @DisplayName("파일이 없는 경우")
        class WhenFileNotFound {

            @Test
            @DisplayName("DrinkImportSourceReadFailedException을 던진다")
            void shouldThrowDrinkImportSourceReadFailedException() {
                // given
                CsvXWineReader csvXWineReader =
                        new CsvXWineReader(new XWinesProperties("not-exists/x-wines.csv"));

                // when & then
                assertThatThrownBy(csvXWineReader::read)
                        .isInstanceOf(DrinkImportSourceReadFailedException.class);
            }
        }
    }
}
