package com.symphonia.pairing.infrastructure.reader;

import com.symphonia.pairing.domain.exception.DrinkImportSourceReadFailedException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectReader;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

// 규칙표와 매핑표처럼 버전 관리되는 클래스패스 CSV를 헤더 기준으로 읽는다.
// 읽기, 파싱, 숫자 변환 실패와 빠진 열은 모두 적재 실패 예외로 바꾼다.
// ObjectReader는 불변이고 스레드에 안전하다. 그래서 한 번만 만들어 모든 CSV에 재사용한다.
public final class ClasspathCsv {
    private static final ObjectReader ROW_READER =
            new CsvMapper().readerForMapOf(String.class).with(CsvSchema.emptySchema().withHeader());

    private ClasspathCsv() {}

    // CSV는 수 KB라 통째로 읽는다. 파일 스트림은 getContentAsByteArray가 열고 닫는다.
    public static <T> List<T> readRows(String path, Function<Map<String, String>, T> toRow) {
        try (MappingIterator<Map<String, String>> rows =
                ROW_READER.readValues(new ClassPathResource(path).getContentAsByteArray())) {
            return rows.readAll().stream().map(toRow).toList();
        } catch (IOException | JacksonException | IllegalArgumentException e) {
            throw new DrinkImportSourceReadFailedException();
        }
    }

    // 헤더 오타나 빠진 열을 NullPointerException이 아니라 적재 실패로 알린다.
    public static String column(Map<String, String> row, String name) {
        return Optional.ofNullable(row.get(name))
                .orElseThrow(DrinkImportSourceReadFailedException::new);
    }
}
