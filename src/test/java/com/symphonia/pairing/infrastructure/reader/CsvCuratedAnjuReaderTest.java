package com.symphonia.pairing.infrastructure.reader;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphonia.pairing.domain.vo.AnjuBase;
import com.symphonia.pairing.domain.vo.AnjuSource;
import com.symphonia.pairing.domain.vo.CuratedAnju;
import com.symphonia.pairing.domain.vo.ServingStyle;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CsvCuratedAnjuReader 단위 테스트")
class CsvCuratedAnjuReaderTest {

    private final CsvCuratedAnjuReader csvCuratedAnjuReader = new CsvCuratedAnjuReader();

    @Nested
    @DisplayName("read 메서드는")
    class Read {

        @Test
        @DisplayName("클래스패스의 매핑표를 모든 행 그대로 읽는다")
        void shouldReadRowCountOfClasspathMappingTable() {
            // when
            List<CuratedAnju> curatedAnjus = csvCuratedAnjuReader.read();

            // then
            assertThat(curatedAnjus).hasSize(185);
        }

        @Test
        @DisplayName("uk_anju_name에 걸리지 않도록 이름이 중복되지 않는다")
        void shouldReadUniqueNames() {
            // when
            List<CuratedAnju> curatedAnjus = csvCuratedAnjuReader.read();

            // then
            assertThat(curatedAnjus).extracting(CuratedAnju::name).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("적재 키인 (source, external_id)가 중복되지 않는다")
        void shouldReadUniqueIdentities() {
            // when
            List<CuratedAnju> curatedAnjus = csvCuratedAnjuReader.read();

            // then
            assertThat(curatedAnjus)
                    .extracting(
                            curatedAnju -> curatedAnju.source() + ":" + curatedAnju.externalId())
                    .doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("식품코드를 고쳐도 적재된 안주를 찾을 수 있도록 모든 행에 사람이 정한 external_id가 있다")
        void shouldReadExternalIdOfEveryRow() {
            // when
            List<CuratedAnju> curatedAnjus = csvCuratedAnjuReader.read();

            // then
            assertThat(curatedAnjus)
                    .allSatisfy(
                            curatedAnju ->
                                    assertThat(curatedAnju.externalId())
                                            .as(curatedAnju.name())
                                            .isNotBlank()
                                            .isNotEqualTo(curatedAnju.kfindCode()));
        }

        @Test
        @DisplayName("채워진 kfind_code가 중복되지 않는다")
        void shouldReadUniqueKfindCodes() {
            // when
            List<CuratedAnju> curatedAnjus = csvCuratedAnjuReader.read();

            // then
            assertThat(curatedAnjus)
                    .extracting(CuratedAnju::kfindCode)
                    .filteredOn(kfindCode -> !kfindCode.isBlank())
                    .doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("시드 행은 V7 마이그레이션이 넣은 external_id를 그대로 쓴다")
        void shouldKeepSeedExternalIdsOfMigration() {
            // when
            List<CuratedAnju> curatedAnjus = csvCuratedAnjuReader.read();

            // then
            assertThat(curatedAnjus)
                    .filteredOn(curatedAnju -> curatedAnju.source() == AnjuSource.SEED)
                    .extracting(CuratedAnju::externalId)
                    .containsExactlyInAnyOrder(
                            "golbaengi-muchim",
                            "fried-chicken",
                            "tofu-kimchi",
                            "dried-snack",
                            "cheese-platter",
                            "fruit-platter");
        }

        @Test
        @DisplayName("각 열을 CuratedAnju의 필드로 변환하고 |로 나눈 재료를 목록으로 읽는다")
        void shouldMapColumnsToCuratedAnjuFields() {
            // when
            List<CuratedAnju> curatedAnjus = csvCuratedAnjuReader.read();

            // then
            assertThat(curatedAnjus)
                    .first()
                    .isEqualTo(
                            new CuratedAnju(
                                    AnjuSource.SEED,
                                    "fried-chicken",
                                    "D312-549000000-0010",
                                    "후라이드치킨",
                                    List.of("닭고기"),
                                    List.of("밀가루", "전분", "달걀"),
                                    AnjuBase.POULTRY,
                                    ServingStyle.MAIN,
                                    true));
        }

        @Test
        @DisplayName("부재료 칸이 비어 있으면 빈 목록으로 읽는다")
        void shouldReadEmptySubIngredientsWhenCellBlank() {
            // when
            List<CuratedAnju> curatedAnjus = csvCuratedAnjuReader.read();

            // then
            assertThat(curatedAnjus)
                    .filteredOn(curatedAnju -> curatedAnju.name().equals("마른안주"))
                    .singleElement()
                    .extracting(CuratedAnju::subIngredients)
                    .isEqualTo(List.of());
        }
    }
}
