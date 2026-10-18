package com.symphonia.pairing.presentation.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import com.symphonia.pairing.application.dto.result.ImportCuratedDrinkResult;
import com.symphonia.pairing.application.dto.result.ImportResult;
import com.symphonia.pairing.application.dto.result.ImportXWineResult;
import com.symphonia.pairing.application.usecase.ImportCuratedDrinkUseCase;
import com.symphonia.pairing.application.usecase.ImportDrinkStyleUseCase;
import com.symphonia.pairing.application.usecase.ImportXWineUseCase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

// 배포 기동에서 실수로 적재가 돌지 않도록 빈 등록 조건과 적재 순서만 가볍게 검증한다.
@DisplayName("ImportCatalogRunner 단위 테스트")
class ImportCatalogRunnerTest {

    private final ImportDrinkStyleUseCase importDrinkStyleUseCase =
            mock(ImportDrinkStyleUseCase.class);
    private final ImportCuratedDrinkUseCase importCuratedDrinkUseCase =
            mock(ImportCuratedDrinkUseCase.class);
    private final ImportXWineUseCase importXWineUseCase = mock(ImportXWineUseCase.class);

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withBean(ImportDrinkStyleUseCase.class, () -> importDrinkStyleUseCase)
                    .withBean(ImportCuratedDrinkUseCase.class, () -> importCuratedDrinkUseCase)
                    .withBean(ImportXWineUseCase.class, () -> importXWineUseCase)
                    .withUserConfiguration(ImportCatalogRunner.class);

    @Nested
    @DisplayName("빈 등록은")
    class BeanRegistration {

        @Nested
        @DisplayName("import.catalog.enabled가 true인 경우")
        class WhenEnabled {

            @Test
            @DisplayName("ImportCatalogRunner 빈을 만든다")
            void shouldRegisterRunner() {
                // when & then
                contextRunner
                        .withPropertyValues("import.catalog.enabled=true")
                        .run(
                                context ->
                                        assertThat(context)
                                                .hasSingleBean(ImportCatalogRunner.class));
            }
        }

        @Nested
        @DisplayName("import.catalog.enabled 속성이 없는 경우")
        class WhenPropertyMissing {

            @Test
            @DisplayName("ImportCatalogRunner 빈을 만들지 않는다")
            void shouldNotRegisterRunner() {
                // when & then
                contextRunner.run(
                        context -> assertThat(context).doesNotHaveBean(ImportCatalogRunner.class));
            }
        }

        @Nested
        @DisplayName("import.catalog.enabled가 false인 경우")
        class WhenDisabled {

            @Test
            @DisplayName("ImportCatalogRunner 빈을 만들지 않는다")
            void shouldNotRegisterRunner() {
                // when & then
                contextRunner
                        .withPropertyValues("import.catalog.enabled=false")
                        .run(
                                context ->
                                        assertThat(context)
                                                .doesNotHaveBean(ImportCatalogRunner.class));
            }
        }

        @Nested
        @DisplayName("예전 속성 import.drink.enabled만 true인 경우")
        class WhenOnlyLegacyPropertyEnabled {

            @Test
            @DisplayName("ImportCatalogRunner 빈을 만들지 않는다")
            void shouldNotRegisterRunner() {
                // when & then
                contextRunner
                        .withPropertyValues("import.drink.enabled=true")
                        .run(
                                context ->
                                        assertThat(context)
                                                .doesNotHaveBean(ImportCatalogRunner.class));
            }
        }
    }

    @Nested
    @DisplayName("run 메서드는")
    class Run {

        @Test
        @DisplayName("BJCP 스타일, 국내 유통 맥주, X-Wines 와인 순서로 적재한다")
        void shouldImportBjcpStylesCuratedBeersAndXWinesInOrder() {
            // given
            given(importDrinkStyleUseCase.importBjcpStyles())
                    .willReturn(new ImportResult(1, List.of(), Map.of()));
            given(importCuratedDrinkUseCase.importCuratedBeers())
                    .willReturn(new ImportCuratedDrinkResult(1, List.of()));
            given(importXWineUseCase.importXWines())
                    .willReturn(new ImportXWineResult(1, 1, List.of(), List.of()));
            ImportCatalogRunner runner =
                    new ImportCatalogRunner(
                            importDrinkStyleUseCase, importCuratedDrinkUseCase, importXWineUseCase);

            // when
            runner.run(new DefaultApplicationArguments());

            // then
            InOrder inOrder =
                    inOrder(importDrinkStyleUseCase, importCuratedDrinkUseCase, importXWineUseCase);
            inOrder.verify(importDrinkStyleUseCase).importBjcpStyles();
            inOrder.verify(importCuratedDrinkUseCase).importCuratedBeers();
            inOrder.verify(importXWineUseCase).importXWines();
        }
    }
}
