package com.symphonia.catalog.infrastructure.reader;

import static com.symphonia.catalog.infrastructure.reader.HeaderCsv.column;
import static com.symphonia.catalog.infrastructure.reader.HeaderCsv.readRows;
import static com.symphonia.catalog.infrastructure.reader.HeaderCsv.values;
import static java.util.stream.Collectors.toSet;

import com.symphonia.catalog.domain.reader.WineFlavorRuleReader;
import com.symphonia.catalog.domain.vo.FlavorProfile;
import com.symphonia.catalog.domain.vo.Harmonize;
import com.symphonia.catalog.domain.vo.WineAcidityRule;
import com.symphonia.catalog.domain.vo.WineBodyRule;
import com.symphonia.catalog.domain.vo.WineFlavorRule;
import com.symphonia.catalog.domain.vo.WineHarmonizeRule;
import com.symphonia.catalog.domain.vo.WineTypeRule;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

// 규칙표는 버전 관리되는 설정이라 클래스패스(import/rules)에서 읽는다.
@Component
public class CsvWineFlavorRuleReader implements WineFlavorRuleReader {
    private static final String RULES_DIR = "import/rules/";

    @Override
    public WineFlavorRule read() {
        return new WineFlavorRule(
                readRows(new ClassPathResource(RULES_DIR + "wine-type.csv"), this::toTypeRule),
                readRows(new ClassPathResource(RULES_DIR + "wine-body.csv"), this::toBodyRule),
                readRows(
                        new ClassPathResource(RULES_DIR + "wine-acidity.csv"), this::toAcidityRule),
                readRows(
                        new ClassPathResource(RULES_DIR + "wine-harmonize.csv"),
                        this::toHarmonizeRule));
    }

    private WineTypeRule toTypeRule(Map<String, String> row) {
        return new WineTypeRule(
                column(row, "type"),
                column(row, "external_id"),
                column(row, "name"),
                FlavorProfile.of(
                        Integer.parseInt(column(row, "sweetness")),
                        Integer.parseInt(column(row, "bitterness")),
                        Integer.parseInt(column(row, "carbonation")),
                        Integer.parseInt(column(row, "richness")),
                        Integer.parseInt(column(row, "acidity"))));
    }

    private WineBodyRule toBodyRule(Map<String, String> row) {
        return new WineBodyRule(column(row, "body"), Integer.parseInt(column(row, "richness")));
    }

    private WineAcidityRule toAcidityRule(Map<String, String> row) {
        return new WineAcidityRule(column(row, "acidity"), Integer.parseInt(column(row, "value")));
    }

    // 맞는 계열이 없는 태그는 칸을 비워 두고, 둘 이상에 걸치는 태그는 |로 나눠 적는다.
    private WineHarmonizeRule toHarmonizeRule(Map<String, String> row) {
        return new WineHarmonizeRule(
                column(row, "tag"),
                values(column(row, "harmonizes")).stream()
                        .map(Harmonize::valueOf)
                        .collect(toSet()));
    }
}
