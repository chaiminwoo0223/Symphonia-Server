package com.symphonia.pairing.application.service;

import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.pairing.application.dto.result.ImportAnjuResult;
import com.symphonia.pairing.application.usecase.ImportAnjuUseCase;
import com.symphonia.pairing.domain.entity.Anju;
import com.symphonia.pairing.domain.reader.AnjuNutritionReader;
import com.symphonia.pairing.domain.reader.AnjuRuleReader;
import com.symphonia.pairing.domain.reader.CuratedAnjuReader;
import com.symphonia.pairing.domain.repository.AnjuRepository;
import com.symphonia.pairing.domain.repository.PairingFeedbackRepository;
import com.symphonia.pairing.domain.vo.AnjuEvaluation;
import com.symphonia.pairing.domain.vo.AnjuNutrition;
import com.symphonia.pairing.domain.vo.AnjuRule;
import com.symphonia.pairing.domain.vo.AnjuSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@CommandService
@RequiredArgsConstructor
public class ImportAnjuService implements ImportAnjuUseCase {
    private final CuratedAnjuReader curatedAnjuReader;
    private final AnjuNutritionReader anjuNutritionReader;
    private final AnjuRuleReader anjuRuleReader;
    private final AnjuRepository anjuRepository;
    private final PairingFeedbackRepository pairingFeedbackRepository;

    // 맛이나 알레르기를 정할 수 없는 안주는 저장하지 않고 제외 사유와 함께 결과에 남긴다.
    // 이미 적재된 안주는 (source, external_id)로 찾아 갱신하므로 여러 번 실행해도 결과가 같다.
    // 시드 행도 같은 방식으로 찾으므로 pairing_feedback이 참조하는 id가 유지된다.
    // 매핑표에 SEED와 K_FIND가 섞여 있어 기존 안주는 출처를 가리지 않고 알레르기까지 한 번에 불러와 찾는다.
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
        List<Anju> imported =
                evaluationsByDetermined.get(true).stream().map(AnjuEvaluation::toAnju).toList();

        Map<AnjuIdentity, Anju> existingByIdentity =
                anjuRepository.findAll().stream()
                        .collect(toMap(AnjuIdentity::of, Function.identity()));
        Map<Boolean, List<Anju>> staleByReferenced =
                staleByReferenced(imported, existingByIdentity);
        List<Anju> deleted = staleByReferenced.get(false);
        anjuRepository.deleteAllById(deleted.stream().map(Anju::getId).toList());

        List<Anju> importedAnjus =
                imported.stream().map(anju -> upsert(anju, existingByIdentity)).toList();
        return ImportAnjuResult.of(
                importedAnjus,
                evaluationsByDetermined.get(false),
                deleted,
                staleByReferenced.get(true));
    }

    // 적재가 소유한 출처의 안주 중 이번에 적재하지 않은 안주는 매핑표에서 빠졌거나 맛을 정할 수 없게 된 안주다.
    // 이런 안주가 예전 맛 값으로 계속 추천되지 않도록 지운다.
    // 피드백이 참조하는 안주(true)는 FK 때문에 지울 수 없어 남기고, 나머지(false)만 지운다.
    private Map<Boolean, List<Anju>> staleByReferenced(
            List<Anju> imported, Map<AnjuIdentity, Anju> existingByIdentity) {
        Set<AnjuIdentity> importedIdentities =
                imported.stream().map(AnjuIdentity::of).collect(toSet());
        List<Anju> stale =
                existingByIdentity.values().stream()
                        .filter(anju -> anju.getSource().isManagedByImport())
                        .filter(anju -> !importedIdentities.contains(AnjuIdentity.of(anju)))
                        .toList();
        Set<Long> referencedIds =
                stale.isEmpty()
                        ? Set.of()
                        : pairingFeedbackRepository.findReferencedAnjuIds(
                                stale.stream().map(Anju::getId).toList());

        return stale.stream().collect(partitioningBy(anju -> referencedIds.contains(anju.getId())));
    }

    private Anju upsert(Anju imported, Map<AnjuIdentity, Anju> existingByIdentity) {
        Anju anju =
                Optional.ofNullable(existingByIdentity.get(AnjuIdentity.of(imported)))
                        .map(existing -> existing.overwriteWith(imported))
                        .orElse(imported);
        return anjuRepository.save(anju);
    }

    private record AnjuIdentity(AnjuSource source, String externalId) {
        private static AnjuIdentity of(Anju anju) {
            return new AnjuIdentity(anju.getSource(), anju.getExternalId());
        }
    }
}
