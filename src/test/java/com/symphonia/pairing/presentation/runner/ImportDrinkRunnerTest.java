package com.symphonia.pairing.presentation.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import com.symphonia.pairing.application.dto.result.ImportCuratedDrinkResult;
import com.symphonia.pairing.application.dto.result.ImportResult;
import com.symphonia.pairing.application.usecase.ImportCuratedDrinkUseCase;
import com.symphonia.pairing.application.usecase.ImportDrinkStyleUseCase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

// 배포 기동에서 실수로 적재가 돌지 않도록 빈 등록 조건과 적재 순서만 가볍게 검증한다.
@DisplayName("ImportDrinkRunner 단위 테스트")
class ImportDrinkRunnerTest {

    private final ImportDrinkStyleUseCase importDrinkStyleUseCase =
            mock(ImportDrinkStyleUseCase.class);
    private final ImportCuratedDrinkUseCase importCuratedDrinkUseCase =
            mock(ImportCuratedDrinkUseCase.class);

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withBean(ImportDrinkStyleUseCase.class, () -> importDrinkStyleUseCase)
                    .withBean(ImportCuratedDrinkUseCase.class, () -> importCuratedDrinkUseCase)
                    .withUserConfiguration(ImportDrinkRunner.class);

    @Nested
    @DisplayName("빈 등록은")
    class BeanRegistration {

        @Nested
        @DisplayName("import.drink.enabled가 true인 경우")
        class WhenEnabled {

            @Test
            @DisplayName("ImportDrinkRunner 빈을 만든다")
            void shouldRegisterRunner() {
                // when & then
                contextRunner
                        .withPropertyValues("import.drink.enabled=true")
                        .run(context -> assertThat(context).hasSingleBean(ImportDrinkRunner.class));
            }
        }

        @Nested
        @DisplayName("import.drink.enabled 속성이 없는 경우")
        class WhenPropertyMissing {

            @Test
            @DisplayName("ImportDrinkRunner 빈을 만들지 않는다")
            void shouldNotRegisterRunner() {
                // when & then
                contextRunner.run(
                        context -> assertThat(context).doesNotHaveBean(ImportDrinkRunner.class));
            }
        }

        @Nested
        @DisplayName("import.drink.enabled가 false인 경우")
        class WhenDisabled {

            @Test
            @DisplayName("ImportDrinkRunner 빈을 만들지 않는다")
            void shouldNotRegisterRunner() {
                // when & then
                contextRunner
                        .withPropertyValues("import.drink.enabled=false")
                        .run(
                                context ->
                                        assertThat(context)
                                                .doesNotHaveBean(ImportDrinkRunner.class));
            }
        }

        @Nested
        @DisplayName("예전 속성 import.drink-style.enabled만 true인 경우")
        class WhenOnlyLegacyPropertyEnabled {

            @Test
            @DisplayName("ImportDrinkRunner 빈을 만들지 않는다")
            void shouldNotRegisterRunner() {
                // when & then
                contextRunner
                        .withPropertyValues("import.drink-style.enabled=true")
                        .run(
                                context ->
                                        assertThat(context)
                                                .doesNotHaveBean(ImportDrinkRunner.class));
            }
        }
    }

    @Nested
    @DisplayName("run 메서드는")
    class Run {

        @Test
        @DisplayName("BJCP 스타일을 먼저 적재한 뒤 국내 유통 맥주를 적재한다")
        void shouldImportCuratedBeersAfterBjcpStyles() {
            // given
            given(importDrinkStyleUseCase.importBjcpStyles())
                    .willReturn(new ImportResult(1, List.of(), Map.of()));
            given(importCuratedDrinkUseCase.importCuratedBeers())
                    .willReturn(new ImportCuratedDrinkResult(1, List.of()));
            ImportDrinkRunner runner =
                    new ImportDrinkRunner(importDrinkStyleUseCase, importCuratedDrinkUseCase);

            // when
            runner.run(new DefaultApplicationArguments());

            // then
            InOrder inOrder = inOrder(importDrinkStyleUseCase, importCuratedDrinkUseCase);
            inOrder.verify(importDrinkStyleUseCase).importBjcpStyles();
            inOrder.verify(importCuratedDrinkUseCase).importCuratedBeers();
        }
    }
}
