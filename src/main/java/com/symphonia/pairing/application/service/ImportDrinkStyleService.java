package com.symphonia.pairing.application.service;

import static java.util.stream.Collectors.partitioningBy;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.pairing.application.dto.result.ImportResult;
import com.symphonia.pairing.application.usecase.ImportDrinkStyleUseCase;
import com.symphonia.pairing.domain.entity.DrinkStyle;
import com.symphonia.pairing.domain.reader.BjcpStyleReader;
import com.symphonia.pairing.domain.reader.FlavorRuleReader;
import com.symphonia.pairing.domain.repository.DrinkStyleRepository;
import com.symphonia.pairing.domain.vo.FlavorEvaluation;
import com.symphonia.pairing.domain.vo.FlavorRule;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@CommandService
@RequiredArgsConstructor
public class ImportDrinkStyleService implements ImportDrinkStyleUseCase {
    private final BjcpStyleReader bjcpStyleReader;
    private final FlavorRuleReader flavorRuleReader;
    private final DrinkStyleRepository drinkStyleRepository;

    // 맛을 정할 수 없는 스타일은 적재하지 않고 결과에 남긴다.
    // 이미 적재된 스타일은 (source, external_id)로 찾아 갱신하므로 여러 번 실행해도 결과가 같다.
    @Override
    public ImportResult importBjcpStyles() {
        FlavorRule flavorRule = flavorRuleReader.read();
        Map<Boolean, List<FlavorEvaluation>> evaluationsByDetermined = evaluateStyles(flavorRule);
        List<FlavorEvaluation> determined = evaluationsByDetermined.get(true);
        List<FlavorEvaluation> undetermined = evaluationsByDetermined.get(false);

        determined.forEach(evaluation -> upsert(evaluation.toDrinkStyle()));

        return ImportResult.of(determined, undetermined);
    }

    // 맛을 정할 수 있는 스타일(true)과 정할 수 없는 스타일(false)로 나눈다.
    private Map<Boolean, List<FlavorEvaluation>> evaluateStyles(FlavorRule flavorRule) {
        return bjcpStyleReader.read().stream()
                .map(flavorRule::evaluate)
                .collect(partitioningBy(FlavorEvaluation::isDetermined));
    }

    private void upsert(DrinkStyle imported) {
        drinkStyleRepository
                .findBySourceAndExternalId(imported.getSource(), imported.getExternalId())
                .ifPresentOrElse(
                        existing -> {
                            existing.update(
                                    imported.getName(),
                                    imported.getCategory(),
                                    imported.getFlavorProfile());
                            drinkStyleRepository.save(existing);
                        },
                        () -> drinkStyleRepository.save(imported));
    }
}
