package com.symphonia.catalog.infrastructure.reader;

import static com.symphonia.catalog.infrastructure.reader.HeaderCsv.column;
import static com.symphonia.catalog.infrastructure.reader.HeaderCsv.readRows;
import static com.symphonia.catalog.infrastructure.reader.HeaderCsv.values;
import static java.util.stream.Collectors.toSet;

import com.symphonia.catalog.domain.reader.BeerAllergyRuleReader;
import com.symphonia.catalog.domain.vo.AllergyType;
import com.symphonia.catalog.domain.vo.BeerAllergyRule;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

// 규칙표는 버전 관리되는 설정이라 클래스패스(import/rules)에서 읽는다.
@Component
public class CsvBeerAllergyRuleReader implements BeerAllergyRuleReader {
    private static final String RULES_PATH = "import/rules/beer-allergy.csv";

    @Override
    public List<BeerAllergyRule> read() {
        return readRows(new ClassPathResource(RULES_PATH), this::toRule);
    }

    private BeerAllergyRule toRule(Map<String, String> row) {
        return new BeerAllergyRule(
                column(row, "bjcp_style_id"),
                values(column(row, "allergy_types")).stream()
                        .map(AllergyType::valueOf)
                        .collect(toSet()));
    }
}
