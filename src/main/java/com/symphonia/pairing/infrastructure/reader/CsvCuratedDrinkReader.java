package com.symphonia.pairing.infrastructure.reader;

import com.symphonia.pairing.domain.exception.DrinkImportSourceReadFailedException;
import com.symphonia.pairing.domain.reader.CuratedDrinkReader;
import com.symphonia.pairing.domain.vo.CuratedDrink;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectReader;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

// 매핑표는 사람이 고른 제품 목록이라 규칙표처럼 클래스패스(import/curated)에서 읽는다.
@Component
public class CsvCuratedDrinkReader implements CuratedDrinkReader {
    private static final String BEERS_PATH = "import/curated/beers.csv";
    private static final ObjectReader ROW_READER =
            new CsvMapper().readerForMapOf(String.class).with(CsvSchema.emptySchema().withHeader());

    @Override
    public List<CuratedDrink> read() {
        try (MappingIterator<Map<String, String>> rows =
                ROW_READER.readValues(new ClassPathResource(BEERS_PATH).getContentAsByteArray())) {
            return rows.readAll().stream().map(this::toCuratedDrink).toList();
        } catch (IOException | JacksonException | IllegalArgumentException e) {
            throw new DrinkImportSourceReadFailedException();
        }
    }

    private CuratedDrink toCuratedDrink(Map<String, String> row) {
        return new CuratedDrink(
                column(row, "external_id"),
                column(row, "name"),
                Double.parseDouble(column(row, "abv")),
                column(row, "bjcp_style_id"));
    }

    // 헤더 오타나 빠진 열을 NullPointerException이 아니라 적재 실패로 알린다.
    private String column(Map<String, String> row, String name) {
        return Optional.ofNullable(row.get(name))
                .orElseThrow(DrinkImportSourceReadFailedException::new);
    }
}
