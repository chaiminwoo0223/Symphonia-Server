package com.symphonia.catalog.infrastructure.reader;

import static com.symphonia.catalog.infrastructure.reader.HeaderCsv.column;
import static com.symphonia.catalog.infrastructure.reader.HeaderCsv.readRows;

import com.symphonia.catalog.domain.reader.AnjuNutritionReader;
import com.symphonia.catalog.domain.vo.AnjuNutrition;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

// K-FIND는 이용허락범위 제한이 없어 안주에 쓰는 행만 추출한 영양성분을 클래스패스(import/kfind)에 커밋하고 읽는다.
@Component
public class CsvAnjuNutritionReader implements AnjuNutritionReader {
    private static final String NUTRITION_PATH = "import/kfind/nutrition.csv";

    @Override
    public List<AnjuNutrition> read() {
        return readRows(new ClassPathResource(NUTRITION_PATH), this::toAnjuNutrition);
    }

    private AnjuNutrition toAnjuNutrition(Map<String, String> row) {
        return new AnjuNutrition(
                column(row, "kfind_code"),
                column(row, "food_name"),
                grams(column(row, "sugar_grams")),
                grams(column(row, "fat_grams")),
                column(row, "raw_materials"));
    }

    // K-FIND가 측정하지 않은 성분은 빈 칸으로 추출한다.
    private Double grams(String value) {
        return value.isBlank() ? null : Double.valueOf(value);
    }
}
