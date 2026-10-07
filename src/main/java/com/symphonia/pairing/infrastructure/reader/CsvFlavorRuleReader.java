package com.symphonia.pairing.infrastructure.reader;

import static com.symphonia.pairing.infrastructure.reader.ClasspathCsv.column;
import static com.symphonia.pairing.infrastructure.reader.ClasspathCsv.readRows;

import com.symphonia.pairing.domain.reader.FlavorRuleReader;
import com.symphonia.pairing.domain.vo.FlavorAxis;
import com.symphonia.pairing.domain.vo.FlavorRule;
import com.symphonia.pairing.domain.vo.IbuRule;
import com.symphonia.pairing.domain.vo.MouthfeelRule;
import com.symphonia.pairing.domain.vo.TagRule;
import java.util.Map;
import org.springframework.stereotype.Component;

// 규칙표는 버전 관리되는 설정이라 클래스패스(import/rules)에서 읽는다.
@Component
public class CsvFlavorRuleReader implements FlavorRuleReader {
    private static final String RULES_DIR = "import/rules/";

    @Override
    public FlavorRule read() {
        return new FlavorRule(
                readRows(RULES_DIR + "ibu-bitterness.csv", this::toIbuRule),
                readRows(RULES_DIR + "tag.csv", this::toTagRule),
                readRows(RULES_DIR + "mouthfeel.csv", this::toMouthfeelRule));
    }

    private IbuRule toIbuRule(Map<String, String> row) {
        return new IbuRule(
                Double.parseDouble(column(row, "min_ibu")),
                Double.parseDouble(column(row, "max_ibu")),
                Integer.parseInt(column(row, "bitterness")));
    }

    private TagRule toTagRule(Map<String, String> row) {
        return new TagRule(
                column(row, "tag"),
                FlavorAxis.from(column(row, "axis")),
                Integer.parseInt(column(row, "value")));
    }

    private MouthfeelRule toMouthfeelRule(Map<String, String> row) {
        return new MouthfeelRule(
                column(row, "keyword"),
                FlavorAxis.from(column(row, "axis")),
                Integer.parseInt(column(row, "value")));
    }
}
