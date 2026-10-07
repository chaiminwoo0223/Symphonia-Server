package com.symphonia.pairing.infrastructure.reader;

import static com.symphonia.pairing.infrastructure.reader.ClasspathCsv.column;
import static com.symphonia.pairing.infrastructure.reader.ClasspathCsv.readRows;

import com.symphonia.pairing.domain.reader.CuratedDrinkReader;
import com.symphonia.pairing.domain.vo.CuratedDrink;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

// 매핑표는 사람이 고른 제품 목록이라 규칙표처럼 클래스패스(import/curated)에서 읽는다.
@Component
public class CsvCuratedDrinkReader implements CuratedDrinkReader {
    private static final String BEERS_PATH = "import/curated/beers.csv";

    @Override
    public List<CuratedDrink> read() {
        return readRows(BEERS_PATH, this::toCuratedDrink);
    }

    private CuratedDrink toCuratedDrink(Map<String, String> row) {
        return new CuratedDrink(
                column(row, "external_id"),
                column(row, "name"),
                Double.parseDouble(column(row, "abv")),
                column(row, "bjcp_style_id"));
    }
}
