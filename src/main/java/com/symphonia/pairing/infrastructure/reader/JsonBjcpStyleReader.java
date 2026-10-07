package com.symphonia.pairing.infrastructure.reader;

import static java.util.function.Predicate.not;

import com.symphonia.pairing.domain.exception.DrinkImportSourceReadFailedException;
import com.symphonia.pairing.domain.reader.BjcpStyleReader;
import com.symphonia.pairing.domain.vo.BjcpStyle;
import com.symphonia.pairing.domain.vo.IbuRange;
import com.symphonia.pairing.infrastructure.reader.config.properties.BjcpProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// beerjson/bjcp-json 형식의 BJCP 가이드라인 JSON을 읽는다.
@Component
@RequiredArgsConstructor
public class JsonBjcpStyleReader implements BjcpStyleReader {
    private final JsonMapper jsonMapper;
    private final BjcpProperties bjcpProperties;

    @Override
    public List<BjcpStyle> read() {
        try (InputStream inputStream = Files.newInputStream(Path.of(bjcpProperties.path()))) {
            JsonNode styles = jsonMapper.readTree(inputStream).path("beerjson").path("styles");

            return StreamSupport.stream(styles.spliterator(), false).map(this::toStyle).toList();
        } catch (IOException | JacksonException e) {
            throw new DrinkImportSourceReadFailedException();
        }
    }

    private BjcpStyle toStyle(JsonNode style) {
        return new BjcpStyle(
                style.path("style_id").asString(),
                style.path("name").asString(),
                ibuRange(style.path("international_bitterness_units")).orElse(null),
                tags(style.path("tags")),
                style.path("mouthfeel").asString(null));
    }

    // 최솟값과 최댓값 중 하나라도 없으면 구간을 정할 수 없다.
    private Optional<IbuRange> ibuRange(JsonNode ibu) {
        JsonNode min = ibu.at("/minimum/value");
        JsonNode max = ibu.at("/maximum/value");

        return min.isNumber() && max.isNumber()
                ? Optional.of(new IbuRange(min.asDouble(), max.asDouble()))
                : Optional.empty();
    }

    // 태그는 배열이 아니라 "session-strength, pale-color" 형식의 쉼표 구분 문자열이다.
    private List<String> tags(JsonNode node) {
        return node.stringValueOpt().stream()
                .flatMap(tags -> Arrays.stream(tags.split(",")))
                .map(String::strip)
                .filter(not(String::isEmpty))
                .toList();
    }
}
