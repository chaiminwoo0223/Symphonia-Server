package com.symphonia.pairing.application.service;

import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toMap;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.pairing.application.dto.result.ImportAnjuResult;
import com.symphonia.pairing.application.usecase.ImportAnjuUseCase;
import com.symphonia.pairing.domain.entity.Anju;
import com.symphonia.pairing.domain.reader.AnjuNutritionReader;
import com.symphonia.pairing.domain.reader.AnjuRuleReader;
import com.symphonia.pairing.domain.reader.CuratedAnjuReader;
import com.symphonia.pairing.domain.repository.AnjuRepository;
import com.symphonia.pairing.domain.vo.AnjuEvaluation;
import com.symphonia.pairing.domain.vo.AnjuNutrition;
import com.symphonia.pairing.domain.vo.AnjuRule;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@CommandService
@RequiredArgsConstructor
public class ImportAnjuService implements ImportAnjuUseCase {
    private final CuratedAnjuReader curatedAnjuReader;
    private final AnjuNutritionReader anjuNutritionReader;
    private final AnjuRuleReader anjuRuleReader;
    private final AnjuRepository anjuRepository;

    // 맛이나 알레르기를 정할 수 없는 안주는 저장하지 않고 제외 사유와 함께 결과에 남긴다.
    // 이미 적재된 안주는 (source, external_id)로 찾아 갱신하므로 여러 번 실행해도 결과가 같다.
    // 시드 행도 같은 방식으로 찾으므로 pairing_feedback이 참조하는 id가 유지된다.
    @Override
    public ImportAnjuResult importCuratedAnjus() {
        AnjuRule anjuRule = anjuRuleReader.read();
        Map<String, AnjuNutrition> nutritionsByCode =
                anjuNutritionReader.read().stream()
                        .collect(toMap(AnjuNutrition::kfindCode, Function.identity()));

        Map<Boolean, List<AnjuEvaluation>> evaluationsByDetermined =
                curatedAnjuReader.read().stream()
                        .map(
                                curatedAnju ->
                                        anjuRule.evaluate(
                                                curatedAnju,
                                                Optional.ofNullable(
                                                        nutritionsByCode.get(
                                                                curatedAnju.kfindCode()))))
                        .collect(partitioningBy(AnjuEvaluation::isDetermined));

        List<Anju> importedAnjus =
                evaluationsByDetermined.get(true).stream()
                        .map(AnjuEvaluation::toAnju)
                        .map(this::upsert)
                        .toList();
        return ImportAnjuResult.of(importedAnjus, evaluationsByDetermined.get(false));
    }

    private Anju upsert(Anju imported) {
        Anju anju =
                anjuRepository
                        .findBySourceAndExternalId(imported.getSource(), imported.getExternalId())
                        .map(existing -> existing.overwriteWith(imported))
                        .orElse(imported);
        return anjuRepository.save(anju);
    }
}
