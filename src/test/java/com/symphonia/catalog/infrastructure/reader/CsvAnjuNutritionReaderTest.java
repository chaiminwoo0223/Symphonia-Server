package com.symphonia.catalog.infrastructure.reader;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.catalog.domain.vo.AnjuNutrition;
import com.symphonia.catalog.domain.vo.CuratedAnju;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CsvAnjuNutritionReader 단위 테스트")
class CsvAnjuNutritionReaderTest {

    private final CsvAnjuNutritionReader csvAnjuNutritionReader = new CsvAnjuNutritionReader();

    @Nested
    @DisplayName("read 메서드는")
    class Read {

        @Test
        @DisplayName("적재 서비스가 식품코드로 찾을 수 있도록 kfind_code가 중복되지 않는다")
        void shouldReadUniqueKfindCodes() {
            // when
            List<AnjuNutrition> nutritions = csvAnjuNutritionReader.read();

            // then
            assertThat(nutritions).extracting(AnjuNutrition::kfindCode).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("각 열을 AnjuNutrition의 필드로 변환한다")
        void shouldMapColumnsToAnjuNutritionFields() {
            // when
            List<AnjuNutrition> nutritions = csvAnjuNutritionReader.read();

            // then
            assertThat(nutritions)
                    .contains(
                            new AnjuNutrition(
                                    "D312-549000000-0010", "닭튀김_간편조리세트_후라이드치킨", 0.42, 19.68, ""));
        }

        @Test
        @DisplayName("매핑표에 채운 식품코드의 영양성분이 모두 있어 영양성분이 없어 제외되는 안주가 없다")
        void shouldCoverKfindCodesOfMappingTable() {
            // given
            List<String> kfindCodes =
                    new CsvCuratedAnjuReader()
                            .read().stream()
                                    .map(CuratedAnju::kfindCode)
                                    .filter(kfindCode -> !kfindCode.isBlank())
                                    .toList();

            // when
            List<AnjuNutrition> nutritions = csvAnjuNutritionReader.read();

            // then
            assertThat(nutritions).extracting(AnjuNutrition::kfindCode).containsAll(kfindCodes);
        }

        @Test
        @DisplayName("모든 행에 당류와 지방이 있어 측정되지 않아 제외되는 안주가 없다")
        void shouldReadSugarAndFatOfEveryRow() {
            // when
            List<AnjuNutrition> nutritions = csvAnjuNutritionReader.read();

            // then
            assertThat(nutritions)
                    .allSatisfy(
                            nutrition -> {
                                assertThat(nutrition.sugar()).as(nutrition.kfindCode()).isPresent();
                                assertThat(nutrition.fat()).as(nutrition.kfindCode()).isPresent();
                            });
        }
    }
}
