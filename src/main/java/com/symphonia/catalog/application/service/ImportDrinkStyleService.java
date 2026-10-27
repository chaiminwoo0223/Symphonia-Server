package com.symphonia.catalog.application.service;

import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toMap;

import com.symphonia.catalog.application.dto.result.ImportDrinkStyleResult;
import com.symphonia.catalog.application.usecase.ImportDrinkStyleUseCase;
import com.symphonia.catalog.domain.entity.DrinkStyle;
import com.symphonia.catalog.domain.reader.BjcpStyleReader;
import com.symphonia.catalog.domain.reader.FlavorRuleReader;
import com.symphonia.catalog.domain.repository.DrinkStyleRepository;
import com.symphonia.catalog.domain.vo.DrinkStyleSource;
import com.symphonia.catalog.domain.vo.FlavorEvaluation;
import com.symphonia.catalog.domain.vo.FlavorRule;
import com.symphonia.common.annotation.CommandService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@CommandService
@RequiredArgsConstructor
public class ImportDrinkStyleService implements ImportDrinkStyleUseCase {
    private final BjcpStyleReader bjcpStyleReader;
    private final FlavorRuleReader flavorRuleReader;
    private final DrinkStyleRepository drinkStyleRepository;

    // 맛을 정할 수 없는 스타일은 적재하지 않고 결과에 남긴다.
    // 이미 적재된 스타일은 (source, external_id)로 찾아 갱신하므로 여러 번 실행해도 결과가 같다.
    // 기존 스타일은 스타일마다 조회하지 않고 한 번에 불러와 external_id로 찾는다.
    @Override
    public ImportDrinkStyleResult importBjcpStyles() {
        FlavorRule flavorRule = flavorRuleReader.read();
        Map<Boolean, List<FlavorEvaluation>> evaluationsByDetermined = evaluateStyles(flavorRule);
        List<FlavorEvaluation> determined = evaluationsByDetermined.get(true);
        List<FlavorEvaluation> undetermined = evaluationsByDetermined.get(false);

        Map<String, DrinkStyle> existingByExternalId =
                drinkStyleRepository.findAllBySource(DrinkStyleSource.BJCP).stream()
                        .collect(toMap(DrinkStyle::getExternalId, Function.identity()));

        determined.forEach(evaluation -> upsert(evaluation.toDrinkStyle(), existingByExternalId));

        return ImportDrinkStyleResult.of(determined, undetermined);
    }

    // 맛을 정할 수 있는 스타일(true)과 정할 수 없는 스타일(false)로 나눈다.
    private Map<Boolean, List<FlavorEvaluation>> evaluateStyles(FlavorRule flavorRule) {
        return bjcpStyleReader.read().stream()
                .map(flavorRule::evaluate)
                .collect(partitioningBy(FlavorEvaluation::isDetermined));
    }

    private void upsert(DrinkStyle imported, Map<String, DrinkStyle> existingByExternalId) {
        DrinkStyle drinkStyle =
                Optional.ofNullable(existingByExternalId.get(imported.getExternalId()))
                        .map(existing -> existing.overwriteWith(imported))
                        .orElse(imported);
        drinkStyleRepository.save(drinkStyle);
    }
}
