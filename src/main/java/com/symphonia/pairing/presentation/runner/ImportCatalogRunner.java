package com.symphonia.pairing.presentation.runner;

import com.symphonia.pairing.application.dto.result.ImportCuratedDrinkResult;
import com.symphonia.pairing.application.dto.result.ImportCuratedDrinkResult.UnmappedItem;
import com.symphonia.pairing.application.dto.result.ImportResult;
import com.symphonia.pairing.application.dto.result.ImportResult.ExcludedItem;
import com.symphonia.pairing.application.dto.result.ImportXWineResult;
import com.symphonia.pairing.application.dto.result.ImportXWineResult.AbvExceededItem;
import com.symphonia.pairing.application.dto.result.ImportXWineResult.UndeterminedItem;
import com.symphonia.pairing.application.usecase.ImportCuratedDrinkUseCase;
import com.symphonia.pairing.application.usecase.ImportDrinkStyleUseCase;
import com.symphonia.pairing.application.usecase.ImportXWineUseCase;
import com.symphonia.pairing.domain.vo.FlavorAxis;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// 적재는 배포와 분리해 ./gradlew importCatalog로 수동 실행한다.
// 속성이 없는 일반 기동에서는 빈이 만들어지지 않아 적재가 돌지 않는다.
// 제품은 매핑한 스타일을 참조하므로 스타일을 먼저 적재한다. 유스케이스마다 트랜잭션이 따로라 스타일 적재가 먼저 커밋된다.
// 와인은 스타일이 와인 데이터에서만 나오므로 와인 적재가 스타일까지 한 트랜잭션에서 적재한다.
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "import.catalog.enabled", havingValue = "true")
public class ImportCatalogRunner implements ApplicationRunner {
    private final ImportDrinkStyleUseCase importDrinkStyleUseCase;
    private final ImportCuratedDrinkUseCase importCuratedDrinkUseCase;
    private final ImportXWineUseCase importXWineUseCase;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        reportStyles(importDrinkStyleUseCase.importBjcpStyles());
        reportCuratedDrinks(importCuratedDrinkUseCase.importCuratedBeers());
        reportXWines(importXWineUseCase.importXWines());
    }

    private static void reportStyles(ImportResult result) {
        log.info("[ImportCatalog] 적재한 BJCP 스타일: {}개", result.importedCount());
        log.info("[ImportCatalog] 맛을 정할 수 없어 제외한 스타일: {}개", result.excludedItems().size());

        result.excludedItems().forEach(ImportCatalogRunner::logExcludedItem);
        result.defaultedExternalIds().forEach(ImportCatalogRunner::logDefaultedAxis);
    }

    private static void logExcludedItem(ExcludedItem item) {
        log.info(
                "[ImportCatalog]   {} {} (규칙 없는 축: {})",
                item.externalId(),
                item.name(),
                item.missingAxes());
    }

    private static void logDefaultedAxis(FlavorAxis axis, List<String> externalIds) {
        log.info("[ImportCatalog] 기본값 1을 쓴 {}: {}개 {}", axis, externalIds.size(), externalIds);
    }

    private static void reportCuratedDrinks(ImportCuratedDrinkResult result) {
        log.info("[ImportCatalog] 적재한 국내 유통 맥주: {}개", result.importedCount());
        log.info("[ImportCatalog] 매핑한 스타일이 없어 제외한 맥주: {}개", result.unmappedItems().size());

        result.unmappedItems().forEach(ImportCatalogRunner::logUnmappedItem);
    }

    private static void logUnmappedItem(UnmappedItem item) {
        log.info(
                "[ImportCatalog]   {} {} (BJCP 스타일: {})",
                item.externalId(),
                item.name(),
                item.bjcpStyleId());
    }

    private static void reportXWines(ImportXWineResult result) {
        log.info("[ImportCatalog] 적재한 와인 스타일: {}개", result.importedStyleCount());
        log.info("[ImportCatalog] 적재한 X-Wines 와인: {}개", result.importedDrinkCount());
        log.info("[ImportCatalog] 도수 상한을 넘어 제외한 와인: {}개", result.abvExceededItems().size());
        log.info("[ImportCatalog] 맛을 정할 수 없어 제외한 와인: {}개", result.undeterminedItems().size());

        result.abvExceededItems().forEach(ImportCatalogRunner::logAbvExceededItem);
        result.undeterminedItems().forEach(ImportCatalogRunner::logUndeterminedItem);
    }

    private static void logAbvExceededItem(AbvExceededItem item) {
        log.info("[ImportCatalog]   {} {} (도수: {}%)", item.externalId(), item.name(), item.abv());
    }

    private static void logUndeterminedItem(UndeterminedItem item) {
        log.info(
                "[ImportCatalog]   {} {} (규칙 없는 속성: {})",
                item.externalId(),
                item.name(),
                item.unknownAttributes());
    }
}
