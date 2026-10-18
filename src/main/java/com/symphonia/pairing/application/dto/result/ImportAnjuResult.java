package com.symphonia.pairing.application.dto.result;

import com.symphonia.pairing.domain.entity.Anju;
import com.symphonia.pairing.domain.vo.AnjuEvaluation;
import com.symphonia.pairing.domain.vo.AnjuExclusionReason;
import com.symphonia.pairing.domain.vo.Harmonize;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public record ImportAnjuResult(
        int importedCount,
        List<ExcludedItem> excludedItems,
        Map<Harmonize, Integer> harmonizeCounts) {
    public ImportAnjuResult {
        excludedItems = List.copyOf(excludedItems);
        harmonizeCounts = immutableEnumMap(harmonizeCounts);
    }

    // 맞는 안주가 없는 태그도 0으로 남겨, 매핑표에서 빠진 계열을 리포트에서 바로 알 수 있게 한다.
    public static ImportAnjuResult of(List<Anju> importedAnjus, List<AnjuEvaluation> excluded) {
        Map<Harmonize, Integer> harmonizeCounts = new EnumMap<>(Harmonize.class);
        Arrays.stream(Harmonize.values()).forEach(harmonize -> harmonizeCounts.put(harmonize, 0));
        importedAnjus.stream()
                .flatMap(anju -> Harmonize.matching(anju).stream())
                .forEach(harmonize -> harmonizeCounts.merge(harmonize, 1, Integer::sum));

        return new ImportAnjuResult(
                importedAnjus.size(),
                excluded.stream().map(ExcludedItem::from).toList(),
                harmonizeCounts);
    }

    // 규칙표에 추가해야 할 재료를 한 번에 보도록 제외된 안주 전체에서 모아 이름순으로 돌려준다.
    public Set<String> unknownIngredients() {
        Set<String> unknownIngredients = new TreeSet<>();
        excludedItems.forEach(item -> unknownIngredients.addAll(item.unknownIngredients()));
        return Collections.unmodifiableSet(unknownIngredients);
    }

    // 리포트에 태그가 늘 같은 순서로 나오도록 enum 선언 순서를 유지한다.
    private static Map<Harmonize, Integer> immutableEnumMap(Map<Harmonize, Integer> counts) {
        Map<Harmonize, Integer> copy = new EnumMap<>(Harmonize.class);
        copy.putAll(counts);
        return Collections.unmodifiableMap(copy);
    }

    public record ExcludedItem(
            String externalId,
            String name,
            Set<AnjuExclusionReason> reasons,
            Set<String> unknownIngredients) {
        public static ExcludedItem from(AnjuEvaluation evaluation) {
            return new ExcludedItem(
                    evaluation.curatedAnju().externalId(),
                    evaluation.curatedAnju().name(),
                    evaluation.exclusionReasons(),
                    evaluation.unknownIngredients());
        }
    }
}
