package com.symphonia.pairing.infrastructure.reader;

import static com.symphonia.pairing.infrastructure.reader.HeaderCsv.column;
import static com.symphonia.pairing.infrastructure.reader.HeaderCsv.readRows;
import static com.symphonia.pairing.infrastructure.reader.HeaderCsv.values;

import com.symphonia.pairing.domain.reader.AnjuRuleReader;
import com.symphonia.pairing.domain.vo.AllergyType;
import com.symphonia.pairing.domain.vo.AnjuRule;
import com.symphonia.pairing.domain.vo.IngredientRule;
import com.symphonia.pairing.domain.vo.NutrientRule;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

// 규칙표는 버전 관리되는 설정이라 클래스패스(import/rules)에서 읽는다.
@Component
public class CsvAnjuRuleReader implements AnjuRuleReader {
    private static final String RULES_DIR = "import/rules/";

    @Override
    public AnjuRule read() {
        return new AnjuRule(
                nutrientRules("anju-sugar.csv", "sweetness"),
                nutrientRules("anju-fat.csv", "richness"),
                readRows(
                        new ClassPathResource(RULES_DIR + "ingredients.csv"),
                        this::toIngredientRule));
    }

    private List<NutrientRule> nutrientRules(String fileName, String valueColumn) {
        return readRows(
                new ClassPathResource(RULES_DIR + fileName),
                row ->
                        new NutrientRule(
                                Double.parseDouble(column(row, "min_grams")),
                                Double.parseDouble(column(row, "max_grams")),
                                Integer.parseInt(column(row, valueColumn))));
    }

    private IngredientRule toIngredientRule(Map<String, String> row) {
        return new IngredientRule(
                column(row, "keyword"),
                values(column(row, "allergy_types")).stream()
                        .map(AllergyType::valueOf)
                        .collect(Collectors.toSet()),
                Boolean.parseBoolean(column(row, "spicy")),
                Integer.parseInt(column(row, "acidity")),
                Integer.parseInt(column(row, "bitterness")));
    }
}
