package com.symphonia.pairing.infrastructure.reader;

import static com.symphonia.pairing.infrastructure.reader.HeaderCsv.column;
import static com.symphonia.pairing.infrastructure.reader.HeaderCsv.readRows;

import com.symphonia.pairing.domain.reader.XWineReader;
import com.symphonia.pairing.domain.vo.XWine;
import com.symphonia.pairing.infrastructure.reader.config.properties.XWinesProperties;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

// X-Wines 원본은 규칙표가 아니라 적재 대상 데이터라서 클래스패스가 아니라 설정한 파일 경로에서 읽는다.
@Component
@RequiredArgsConstructor
public class CsvXWineReader implements XWineReader {
    private static final Pattern QUOTED_VALUE = Pattern.compile("'([^']*)'");

    private final XWinesProperties xWinesProperties;

    @Override
    public List<XWine> read() {
        return readRows(new FileSystemResource(xWinesProperties.path()), this::toXWine);
    }

    private XWine toXWine(Map<String, String> row) {
        return new XWine(
                column(row, "WineID"),
                column(row, "WineName"),
                column(row, "WineryName"),
                column(row, "Type"),
                Double.parseDouble(column(row, "ABV")),
                column(row, "Body"),
                column(row, "Acidity"),
                harmonizeTags(column(row, "Harmonize")));
    }

    // 원본은 Harmonize를 "['Pasta', 'Veal']"처럼 파이썬 리스트 표기로 적는다. 따옴표 안의 값만 꺼낸다.
    private List<String> harmonizeTags(String cell) {
        return QUOTED_VALUE.matcher(cell).results().map(match -> match.group(1)).toList();
    }
}
