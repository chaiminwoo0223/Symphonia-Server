package com.symphonia.catalog.application.service;

import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toMap;

import com.symphonia.catalog.application.dto.result.ImportXWineResult;
import com.symphonia.catalog.application.usecase.ImportXWineUseCase;
import com.symphonia.catalog.domain.entity.Drink;
import com.symphonia.catalog.domain.entity.DrinkStyle;
import com.symphonia.catalog.domain.reader.WineFlavorRuleReader;
import com.symphonia.catalog.domain.reader.XWineReader;
import com.symphonia.catalog.domain.repository.DrinkRepository;
import com.symphonia.catalog.domain.repository.DrinkStyleRepository;
import com.symphonia.catalog.domain.vo.DrinkSource;
import com.symphonia.catalog.domain.vo.DrinkStyleSource;
import com.symphonia.catalog.domain.vo.WineEvaluation;
import com.symphonia.catalog.domain.vo.WineFlavorRule;
import com.symphonia.catalog.domain.vo.XWine;
import com.symphonia.common.annotation.CommandService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@CommandService
@RequiredArgsConstructor
public class ImportXWineService implements ImportXWineUseCase {
    private final XWineReader xWineReader;
    private final WineFlavorRuleReader wineFlavorRuleReader;
    private final DrinkStyleRepository drinkStyleRepository;
    private final DrinkRepository drinkRepository;

    // 와인 스타일은 X-Wines의 Type에서만 나오므로 같은 트랜잭션에서 스타일을 먼저 적재하고 와인이 그 id를 참조한다.
    // 도수 상한을 넘거나 맛을 정할 수 없는 와인은 적재하지 않고 결과에 남긴다.
    // 이미 적재된 스타일과 와인은 (source, external_id)로 찾아 갱신하므로 여러 번 실행해도 결과가 같다.
    // 기존 스타일과 와인은 행마다 조회하지 않고 한 번에 불러와 external_id로 찾는다.
    @Override
    public ImportXWineResult importXWines() {
        WineFlavorRule wineFlavorRule = wineFlavorRuleReader.read();
        Map<String, DrinkStyle> stylesByExternalId = upsertStyles(wineFlavorRule);

        Map<Boolean, List<XWine>> winesByAbvExceeded =
                xWineReader.read().stream().collect(partitioningBy(XWine::exceedsAbvLimit));
        Map<Boolean, List<WineEvaluation>> evaluationsByDetermined =
                winesByAbvExceeded.get(false).stream()
                        .map(wineFlavorRule::evaluate)
                        .collect(partitioningBy(WineEvaluation::isDetermined));
        List<WineEvaluation> determined = evaluationsByDetermined.get(true);
        Map<String, Drink> existingByExternalId =
                drinkRepository.findAllBySource(DrinkSource.X_WINES).stream()
                        .collect(toMap(Drink::getExternalId, Function.identity()));

        determined.forEach(
                evaluation ->
                        upsert(
                                evaluation.toDrink(
                                        stylesByExternalId.get(evaluation.styleExternalId())),
                                existingByExternalId));

        return ImportXWineResult.of(
                stylesByExternalId.size(),
                determined,
                winesByAbvExceeded.get(true),
                evaluationsByDetermined.get(false));
    }

    private Map<String, DrinkStyle> upsertStyles(WineFlavorRule wineFlavorRule) {
        Map<String, DrinkStyle> existingByExternalId =
                drinkStyleRepository.findAllBySource(DrinkStyleSource.X_WINES).stream()
                        .collect(toMap(DrinkStyle::getExternalId, Function.identity()));

        return wineFlavorRule.toDrinkStyles().stream()
                .map(imported -> upsert(imported, existingByExternalId))
                .collect(toMap(DrinkStyle::getExternalId, Function.identity()));
    }

    private DrinkStyle upsert(DrinkStyle imported, Map<String, DrinkStyle> existingByExternalId) {
        DrinkStyle drinkStyle =
                Optional.ofNullable(existingByExternalId.get(imported.getExternalId()))
                        .map(existing -> existing.overwriteWith(imported))
                        .orElse(imported);
        return drinkStyleRepository.save(drinkStyle);
    }

    private void upsert(Drink imported, Map<String, Drink> existingByExternalId) {
        Drink drink =
                Optional.ofNullable(existingByExternalId.get(imported.getExternalId()))
                        .map(existing -> existing.overwriteWith(imported))
                        .orElse(imported);
        drinkRepository.save(drink);
    }
}
