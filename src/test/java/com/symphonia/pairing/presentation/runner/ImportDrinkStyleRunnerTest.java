package com.symphonia.pairing.presentation.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.symphonia.pairing.application.dto.result.ImportResult;
import com.symphonia.pairing.application.usecase.ImportDrinkStyleUseCase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

// 배포 기동에서 실수로 적재가 돌지 않도록 빈 등록 조건만 가볍게 검증한다.
@DisplayName("ImportDrinkStyleRunner 단위 테스트")
class ImportDrinkStyleRunnerTest {

    private final ImportDrinkStyleUseCase importDrinkStyleUseCase =
            mock(ImportDrinkStyleUseCase.class);

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withBean(ImportDrinkStyleUseCase.class, () -> importDrinkStyleUseCase)
                    .withUserConfiguration(ImportDrinkStyleRunner.class);

    @Nested
    @DisplayName("빈 등록은")
    class BeanRegistration {

        @Nested
        @DisplayName("import.drink-style.enabled가 true인 경우")
        class WhenEnabled {

            @Test
            @DisplayName("ImportDrinkStyleRunner 빈을 만든다")
            void shouldRegisterRunner() {
                // when & then
                contextRunner
                        .withPropertyValues("import.drink-style.enabled=true")
                        .run(
                                context ->
                                        assertThat(context)
                                                .hasSingleBean(ImportDrinkStyleRunner.class));
            }
        }

        @Nested
        @DisplayName("import.drink-style.enabled 속성이 없는 경우")
        class WhenPropertyMissing {

            @Test
            @DisplayName("ImportDrinkStyleRunner 빈을 만들지 않는다")
            void shouldNotRegisterRunner() {
                // when & then
                contextRunner.run(
                        context ->
                                assertThat(context).doesNotHaveBean(ImportDrinkStyleRunner.class));
            }
        }

        @Nested
        @DisplayName("import.drink-style.enabled가 false인 경우")
        class WhenDisabled {

            @Test
            @DisplayName("ImportDrinkStyleRunner 빈을 만들지 않는다")
            void shouldNotRegisterRunner() {
                // when & then
                contextRunner
                        .withPropertyValues("import.drink-style.enabled=false")
                        .run(
                                context ->
                                        assertThat(context)
                                                .doesNotHaveBean(ImportDrinkStyleRunner.class));
            }
        }
    }

    @Nested
    @DisplayName("run 메서드는")
    class Run {

        @Test
        @DisplayName("BJCP 스타일 적재를 실행한다")
        void shouldImportBjcpStyles() {
            // given
            given(importDrinkStyleUseCase.importBjcpStyles())
                    .willReturn(new ImportResult(1, List.of(), Map.of()));
            ImportDrinkStyleRunner runner = new ImportDrinkStyleRunner(importDrinkStyleUseCase);

            // when
            runner.run(new DefaultApplicationArguments());

            // then
            verify(importDrinkStyleUseCase).importBjcpStyles();
        }
    }
}
