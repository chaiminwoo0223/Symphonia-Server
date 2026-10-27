package com.symphonia.catalog.application.dto.result;

import com.symphonia.catalog.domain.entity.Anju;
import com.symphonia.catalog.domain.vo.AnjuEvaluation;
import com.symphonia.catalog.domain.vo.AnjuExclusionReason;
import com.symphonia.catalog.domain.vo.Harmonize;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

public record ImportAnjuResult(
        int importedCount,
        List<ExcludedAnju> excludedAnjus,
        List<RemovedAnju> deletedAnjus,
        List<RemovedAnju> retainedAnjus,
        Map<Harmonize, Integer> harmonizeCounts) {
    public ImportAnjuResult {
        excludedAnjus = List.copyOf(excludedAnjus);
        deletedAnjus = List.copyOf(deletedAnjus);
        retainedAnjus = List.copyOf(retainedAnjus);
        harmonizeCounts = immutableEnumMap(harmonizeCounts);
    }

    // 맞는 안주가 없는 태그도 0으로 남겨, 매핑표에서 빠진 계열을 리포트에서 바로 알 수 있게 한다.
    // deleted는 이번 적재에서 지운 안주이고, retained는 지워야 하지만 피드백이 참조해 남긴 안주다.
    public static ImportAnjuResult of(
            List<Anju> importedAnjus,
            List<AnjuEvaluation> excluded,
            List<Anju> deleted,
            List<Anju> retained) {
        return new ImportAnjuResult(
                importedAnjus.size(),
                excluded.stream().map(ExcludedAnju::from).toList(),
                deleted.stream().map(RemovedAnju::from).toList(),
                retained.stream().map(RemovedAnju::from).toList(),
                harmonizeCounts(importedAnjus));
    }

    // 규칙표에 추가해야 할 재료를 한 번에 보도록 제외된 안주 전체에서 모아 이름순으로 돌려준다.
    public Set<String> unknownIngredients() {
        return excludedAnjus.stream()
                .flatMap(anju -> anju.unknownIngredients().stream())
                .collect(
                        Collectors.collectingAndThen(
                                Collectors.toCollection(TreeSet::new),
                                Collections::unmodifiableSet));
    }

    public record ExcludedAnju(
            String externalId,
            String name,
            Set<AnjuExclusionReason> reasons,
            Set<String> unknownIngredients) {
        public static ExcludedAnju from(AnjuEvaluation evaluation) {
            return new ExcludedAnju(
                    evaluation.curatedAnju().externalId(),
                    evaluation.curatedAnju().name(),
                    evaluation.exclusionReasons(),
                    evaluation.unknownIngredients());
        }
    }

    public record RemovedAnju(String externalId, String name) {
        public static RemovedAnju from(Anju anju) {
            return new RemovedAnju(anju.getExternalId(), anju.getName());
        }
    }

    // 리포트에 태그가 늘 같은 순서로 나오도록 enum 선언 순서를 유지한다.
    private static Map<Harmonize, Integer> immutableEnumMap(Map<Harmonize, Integer> counts) {
        Map<Harmonize, Integer> copy = new EnumMap<>(Harmonize.class);
        copy.putAll(counts);
        return Collections.unmodifiableMap(copy);
    }

    private static Map<Harmonize, Integer> harmonizeCounts(List<Anju> importedAnjus) {
        return Arrays.stream(Harmonize.values())
                .collect(
                        Collectors.toMap(
                                Function.identity(),
                                harmonize ->
                                        (int)
                                                importedAnjus.stream()
                                                        .filter(harmonize::matches)
                                                        .count(),
                                (first, second) -> first,
                                () -> new EnumMap<>(Harmonize.class)));
    }
}
