package com.symphonia.catalog.presentation.runner;

import com.symphonia.catalog.application.dto.result.ImportAnjuResult;
import com.symphonia.catalog.application.dto.result.ImportAnjuResult.ExcludedAnju;
import com.symphonia.catalog.application.dto.result.ImportAnjuResult.RemovedAnju;
import com.symphonia.catalog.application.dto.result.ImportCuratedDrinkResult;
import com.symphonia.catalog.application.dto.result.ImportCuratedDrinkResult.UnmappedDrink;
import com.symphonia.catalog.application.dto.result.ImportDrinkStyleResult;
import com.symphonia.catalog.application.dto.result.ImportDrinkStyleResult.ExcludedStyle;
import com.symphonia.catalog.application.dto.result.ImportXWineResult;
import com.symphonia.catalog.application.dto.result.ImportXWineResult.AbvExceededWine;
import com.symphonia.catalog.application.dto.result.ImportXWineResult.UndeterminedWine;
import com.symphonia.catalog.application.usecase.ImportAnjuUseCase;
import com.symphonia.catalog.application.usecase.ImportCuratedDrinkUseCase;
import com.symphonia.catalog.application.usecase.ImportDrinkStyleUseCase;
import com.symphonia.catalog.application.usecase.ImportXWineUseCase;
import com.symphonia.catalog.domain.vo.FlavorAxis;
import com.symphonia.catalog.domain.vo.Harmonize;
import java.util.List;
import java.util.Set;
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
// 안주는 음료를 참조하지 않아 순서와 무관하다. 트랜잭션이 따로라 안주 적재가 실패해도 이미 커밋된 음료 적재는 남는다.
// 앞 단계가 예외를 던지면 뒤 단계는 실행되지 않으므로, 음료 적재가 실패하면 안주는 적재되지 않는다.
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "import.catalog.enabled", havingValue = "true")
public class ImportCatalogRunner implements ApplicationRunner {
    private final ImportDrinkStyleUseCase importDrinkStyleUseCase;
    private final ImportCuratedDrinkUseCase importCuratedDrinkUseCase;
    private final ImportXWineUseCase importXWineUseCase;
    private final ImportAnjuUseCase importAnjuUseCase;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        reportStyles(importDrinkStyleUseCase.importBjcpStyles());
        reportCuratedDrinks(importCuratedDrinkUseCase.importCuratedBeers());
        reportXWines(importXWineUseCase.importXWines());
        reportAnjus(importAnjuUseCase.importCuratedAnjus());
    }

    private static void reportStyles(ImportDrinkStyleResult result) {
        log.info("[ImportCatalog] 적재한 BJCP 스타일: {}개", result.importedCount());
        log.info("[ImportCatalog] 맛을 정할 수 없어 제외한 스타일: {}개", result.excludedStyles().size());

        result.excludedStyles().forEach(ImportCatalogRunner::logExcludedStyle);
        result.defaultedExternalIds().forEach(ImportCatalogRunner::logDefaultedAxis);
    }

    private static void logExcludedStyle(ExcludedStyle style) {
        log.info(
                "[ImportCatalog]   {} {} (규칙 없는 축: {})",
                style.externalId(),
                style.name(),
                style.missingAxes());
    }

    private static void logDefaultedAxis(FlavorAxis axis, List<String> externalIds) {
        log.info("[ImportCatalog] 기본값 1을 쓴 {}: {}개 {}", axis, externalIds.size(), externalIds);
    }

    private static void reportCuratedDrinks(ImportCuratedDrinkResult result) {
        log.info("[ImportCatalog] 적재한 국내 유통 맥주: {}개", result.importedCount());
        log.info(
                "[ImportCatalog] 매핑한 스타일이나 음식 계열 규칙이 없어 제외한 맥주: {}개",
                result.unmappedDrinks().size());

        result.unmappedDrinks().forEach(ImportCatalogRunner::logUnmappedDrink);
    }

    private static void logUnmappedDrink(UnmappedDrink drink) {
        log.info(
                "[ImportCatalog]   {} {} (BJCP 스타일: {})",
                drink.externalId(),
                drink.name(),
                drink.bjcpStyleId());
    }

    private static void reportXWines(ImportXWineResult result) {
        log.info("[ImportCatalog] 적재한 와인 스타일: {}개", result.importedStyleCount());
        log.info("[ImportCatalog] 적재한 X-Wines 와인: {}개", result.importedDrinkCount());
        log.info("[ImportCatalog] 도수 상한을 넘어 제외한 와인: {}개", result.abvExceededWines().size());
        log.info("[ImportCatalog] 맛을 정할 수 없어 제외한 와인: {}개", result.undeterminedWines().size());

        result.abvExceededWines().forEach(ImportCatalogRunner::logAbvExceededWine);
        result.undeterminedWines().forEach(ImportCatalogRunner::logUndeterminedWine);
    }

    private static void logAbvExceededWine(AbvExceededWine wine) {
        log.info("[ImportCatalog]   {} {} (도수: {}%)", wine.externalId(), wine.name(), wine.abv());
    }

    private static void logUndeterminedWine(UndeterminedWine wine) {
        log.info(
                "[ImportCatalog]   {} {} (규칙 없는 속성: {})",
                wine.externalId(),
                wine.name(),
                wine.unknownAttributes());
    }

    private static void reportAnjus(ImportAnjuResult result) {
        log.info("[ImportCatalog] 적재한 안주: {}개", result.importedCount());
        log.info("[ImportCatalog] 맛이나 알레르기를 정할 수 없어 제외한 안주: {}개", result.excludedAnjus().size());

        result.excludedAnjus().forEach(ImportCatalogRunner::logExcludedAnju);
        log.info("[ImportCatalog] 매핑표에서 빠지거나 제외되어 지운 안주: {}개", result.deletedAnjus().size());
        result.deletedAnjus().forEach(ImportCatalogRunner::logRemovedAnju);
        log.info("[ImportCatalog] 지워야 하지만 피드백이 참조해 남긴 안주: {}개", result.retainedAnjus().size());
        result.retainedAnjus().forEach(ImportCatalogRunner::logRemovedAnju);
        Set<String> unknownIngredients = result.unknownIngredients();
        log.info(
                "[ImportCatalog] 규칙표에 없는 재료: {}개 {}",
                unknownIngredients.size(),
                unknownIngredients);
        result.harmonizeCounts().forEach(ImportCatalogRunner::logHarmonizeCount);
    }

    private static void logExcludedAnju(ExcludedAnju anju) {
        log.info(
                "[ImportCatalog]   {} {} (사유: {})", anju.externalId(), anju.name(), anju.reasons());
    }

    private static void logRemovedAnju(RemovedAnju anju) {
        log.info("[ImportCatalog]   {} {}", anju.externalId(), anju.name());
    }

    private static void logHarmonizeCount(Harmonize harmonize, int count) {
        log.info("[ImportCatalog] Harmonize {}: {}개", harmonize, count);
    }
}
