package com.symphonia.pairing.infrastructure.reader;

import static com.symphonia.pairing.infrastructure.reader.HeaderCsv.column;
import static com.symphonia.pairing.infrastructure.reader.HeaderCsv.readRows;
import static com.symphonia.pairing.infrastructure.reader.HeaderCsv.values;

import com.symphonia.pairing.domain.reader.CuratedAnjuReader;
import com.symphonia.pairing.domain.vo.AnjuBase;
import com.symphonia.pairing.domain.vo.AnjuSource;
import com.symphonia.pairing.domain.vo.CuratedAnju;
import com.symphonia.pairing.domain.vo.ServingStyle;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

// 매핑표는 사람이 고른 안주 목록이라 규칙표처럼 클래스패스(import/curated)에서 읽는다.
@Component
public class CsvCuratedAnjuReader implements CuratedAnjuReader {
    private static final String ANJUS_PATH = "import/curated/anjus.csv";

    @Override
    public List<CuratedAnju> read() {
        return readRows(new ClassPathResource(ANJUS_PATH), this::toCuratedAnju);
    }

    private CuratedAnju toCuratedAnju(Map<String, String> row) {
        return new CuratedAnju(
                AnjuSource.valueOf(column(row, "source")),
                column(row, "external_id"),
                column(row, "kfind_code"),
                column(row, "name"),
                values(column(row, "main_ingredients")),
                values(column(row, "sub_ingredients")),
                AnjuBase.valueOf(column(row, "base")),
                ServingStyle.valueOf(column(row, "serving")));
    }
}
