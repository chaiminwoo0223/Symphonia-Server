package com.symphonia.pairing.presentation.runner;

import com.symphonia.pairing.application.dto.result.ImportResult;
import com.symphonia.pairing.application.dto.result.ImportResult.ExcludedItem;
import com.symphonia.pairing.application.usecase.ImportDrinkStyleUseCase;
import com.symphonia.pairing.domain.vo.FlavorAxis;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// 적재는 배포와 분리해 ./gradlew importDrinkStyles로 수동 실행한다.
// 속성이 없는 일반 기동에서는 빈이 만들어지지 않아 적재가 돌지 않는다.
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "import.drink-style.enabled", havingValue = "true")
public class ImportDrinkStyleRunner implements ApplicationRunner {
    private final ImportDrinkStyleUseCase importDrinkStyleUseCase;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        ImportResult result = importDrinkStyleUseCase.importBjcpStyles();

        log.info("[ImportDrinkStyle] 적재한 BJCP 스타일: {}개", result.importedCount());
        log.info("[ImportDrinkStyle] 맛을 정할 수 없어 제외한 스타일: {}개", result.excludedItems().size());

        result.excludedItems().forEach(ImportDrinkStyleRunner::logExcludedItem);
        result.defaultedExternalIds().forEach(ImportDrinkStyleRunner::logDefaultedAxis);
    }

    private static void logExcludedItem(ExcludedItem item) {
        log.info(
                "[ImportDrinkStyle]   {} {} (규칙 없는 축: {})",
                item.externalId(),
                item.name(),
                item.missingAxes());
    }

    private static void logDefaultedAxis(FlavorAxis axis, List<String> externalIds) {
        log.info("[ImportDrinkStyle] 기본값 1을 쓴 {}: {}개 {}", axis, externalIds.size(), externalIds);
    }
}
