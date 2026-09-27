package com.symphonia.pairing.infrastructure.reader;

import com.symphonia.pairing.domain.exception.DrinkImportSourceReadFailedException;
import com.symphonia.pairing.domain.reader.FlavorRuleReader;
import com.symphonia.pairing.domain.vo.FlavorAxis;
import com.symphonia.pairing.domain.vo.FlavorRule;
import com.symphonia.pairing.domain.vo.IbuRule;
import com.symphonia.pairing.domain.vo.MouthfeelRule;
import com.symphonia.pairing.domain.vo.TagRule;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectReader;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

// 규칙표는 버전 관리되는 설정이라 클래스패스(import/rules)에서 읽는다.
// ObjectReader는 불변이고 스레드에 안전하다. 그래서 한 번만 만들어 모든 규칙표에 재사용한다.
@Component
public class CsvFlavorRuleReader implements FlavorRuleReader {
    private static final String RULES_DIR = "import/rules/";
    private static final ObjectReader ROW_READER =
            new CsvMapper().readerForMapOf(String.class).with(CsvSchema.emptySchema().withHeader());

    @Override
    public FlavorRule read() {
        return new FlavorRule(
                readRows("ibu-bitterness.csv", this::toIbuRule),
                readRows("tag.csv", this::toTagRule),
                readRows("mouthfeel.csv", this::toMouthfeelRule));
    }

    // 규칙표는 수 KB라 통째로 읽는다. 파일 스트림은 getContentAsByteArray가 열고 닫는다.
    private <T> List<T> readRows(String fileName, Function<Map<String, String>, T> toRule) {
        try (MappingIterator<Map<String, String>> rows =
                ROW_READER.readValues(
                        new ClassPathResource(RULES_DIR + fileName).getContentAsByteArray())) {
            return rows.readAll().stream().map(toRule).toList();
        } catch (IOException | JacksonException | IllegalArgumentException e) {
            throw new DrinkImportSourceReadFailedException();
        }
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

    // 헤더 오타나 빠진 열을 NullPointerException이 아니라 적재 실패로 알린다.
    private String column(Map<String, String> row, String name) {
        return Optional.ofNullable(row.get(name))
                .orElseThrow(DrinkImportSourceReadFailedException::new);
    }
}
