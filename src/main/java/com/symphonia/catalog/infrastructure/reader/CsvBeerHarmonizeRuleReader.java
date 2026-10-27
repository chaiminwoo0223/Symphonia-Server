package com.symphonia.catalog.infrastructure.reader;

import static com.symphonia.catalog.infrastructure.reader.HeaderCsv.column;
import static com.symphonia.catalog.infrastructure.reader.HeaderCsv.readRows;
import static com.symphonia.catalog.infrastructure.reader.HeaderCsv.values;
import static java.util.stream.Collectors.toSet;

import com.symphonia.catalog.domain.reader.BeerHarmonizeRuleReader;
import com.symphonia.catalog.domain.vo.BeerHarmonizeRule;
import com.symphonia.catalog.domain.vo.Harmonize;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

// 규칙표는 버전 관리되는 설정이라 클래스패스(import/rules)에서 읽는다.
// ba_style 열은 음식 계열의 근거로 삼은 Brewers Association 표의 스타일이며 적재에는 쓰지 않는다.
@Component
public class CsvBeerHarmonizeRuleReader implements BeerHarmonizeRuleReader {
    private static final String RULES_PATH = "import/rules/beer-harmonize.csv";

    @Override
    public List<BeerHarmonizeRule> read() {
        return readRows(new ClassPathResource(RULES_PATH), this::toRule);
    }

    private BeerHarmonizeRule toRule(Map<String, String> row) {
        return new BeerHarmonizeRule(
                column(row, "bjcp_style_id"),
                values(column(row, "harmonizes")).stream()
                        .map(Harmonize::valueOf)
                        .collect(toSet()));
    }
}
