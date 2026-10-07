package com.symphonia.pairing.infrastructure.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.symphonia.pairing.domain.exception.DrinkImportSourceReadFailedException;
import com.symphonia.pairing.domain.vo.BjcpStyle;
import com.symphonia.pairing.domain.vo.IbuRange;
import com.symphonia.pairing.infrastructure.reader.config.properties.BjcpProperties;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("JsonBjcpStyleReader 단위 테스트")
class JsonBjcpStyleReaderTest {

    private static final String STYLES_RESOURCE = "/import/bjcp-styles.json";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Nested
    @DisplayName("read 메서드는")
    class Read {

        @Nested
        @DisplayName("파일이 있는 경우")
        class WhenFileExists {

            private JsonBjcpStyleReader jsonBjcpStyleReader;

            @BeforeEach
            void setUp() throws URISyntaxException {
                String path =
                        Path.of(
                                        Objects.requireNonNull(
                                                        getClass().getResource(STYLES_RESOURCE))
                                                .toURI())
                                .toString();
                jsonBjcpStyleReader = new JsonBjcpStyleReader(jsonMapper, new BjcpProperties(path));
            }

            @Test
            @DisplayName("모든 스타일을 파일 순서대로 읽는다")
            void shouldReadStylesInOrder() {
                // when
                List<BjcpStyle> styles = jsonBjcpStyleReader.read();

                // then
                assertThat(styles)
                        .extracting(BjcpStyle::styleId)
                        .containsExactly("1A", "27A", "34A");
                assertThat(styles.getFirst().name()).isEqualTo("American Light Lager");
            }

            @Test
            @DisplayName("IBU 최소, 최대값을 읽는다")
            void shouldReadIbu() {
                // when
                List<BjcpStyle> styles = jsonBjcpStyleReader.read();

                // then
                BjcpStyle style = styles.getFirst();
                assertThat(style.ibuRange()).isEqualTo(new IbuRange(8.0, 12.0));
                assertThat(style.ibu()).hasValue(10.0);
            }

            @Test
            @DisplayName("쉼표로 구분된 태그 문자열을 공백을 제거해 나눈다")
            void shouldSplitTags() {
                // when
                List<BjcpStyle> styles = jsonBjcpStyleReader.read();

                // then
                assertThat(styles.getFirst().tags())
                        .containsExactly("session-strength", "pale-color", "bottom-fermented");
            }

            @Test
            @DisplayName("mouthfeel을 소문자로 읽는다")
            void shouldReadMouthfeelInLowercase() {
                // when
                List<BjcpStyle> styles = jsonBjcpStyleReader.read();

                // then
                assertThat(styles.getFirst().mouthfeel())
                        .isEqualTo("very light body, very high carbonation");
            }

            @Test
            @DisplayName("tags, mouthfeel, IBU가 null이면 빈 값으로 읽는다")
            void shouldReadEmptyValuesWhenFieldsNull() {
                // when
                List<BjcpStyle> styles = jsonBjcpStyleReader.read();

                // then
                BjcpStyle style = styles.get(1);
                assertThat(style.tags()).isEmpty();
                assertThat(style.mouthfeel()).isEmpty();
                assertThat(style.ibuRange()).isNull();
                assertThat(style.ibu()).isEmpty();
            }

            @Test
            @DisplayName("tags, mouthfeel, IBU 필드가 없으면 빈 값으로 읽는다")
            void shouldReadEmptyValuesWhenFieldsAbsent() {
                // when
                List<BjcpStyle> styles = jsonBjcpStyleReader.read();

                // then
                BjcpStyle style = styles.get(2);
                assertThat(style.tags()).isEmpty();
                assertThat(style.mouthfeel()).isEmpty();
                assertThat(style.ibu()).isEmpty();
            }
        }

        @Nested
        @DisplayName("파일이 없는 경우")
        class WhenFileNotFound {

            @Test
            @DisplayName("DrinkImportSourceReadFailedException을 던진다")
            void shouldThrowDrinkImportSourceReadFailedException() {
                // given
                JsonBjcpStyleReader reader =
                        new JsonBjcpStyleReader(
                                jsonMapper, new BjcpProperties("not-exists/bjcp-styles.json"));

                // when & then
                assertThatThrownBy(reader::read)
                        .isInstanceOf(DrinkImportSourceReadFailedException.class);
            }
        }
    }
}
