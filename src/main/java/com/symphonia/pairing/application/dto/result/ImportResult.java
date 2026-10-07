package com.symphonia.pairing.application.dto.result;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.symphonia.pairing.domain.vo.FlavorAxis;
import com.symphonia.pairing.domain.vo.FlavorEvaluation;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ImportResult(
        int importedCount,
        List<ExcludedItem> excludedItems,
        Map<FlavorAxis, List<String>> defaultedExternalIds) {
    public ImportResult {
        excludedItems = List.copyOf(excludedItems);
        defaultedExternalIds = immutableEnumMap(defaultedExternalIds);
    }

    // 맛을 정한 스타일은 적재 수와 기본값을 쓴 축으로, 정하지 못한 스타일은 제외 항목으로 남긴다.
    public static ImportResult of(
            List<FlavorEvaluation> determined, List<FlavorEvaluation> undetermined) {
        return new ImportResult(
                determined.size(),
                undetermined.stream().map(ExcludedItem::from).toList(),
                defaultedExternalIds(determined));
    }

    public record ExcludedItem(String externalId, String name, Set<FlavorAxis> missingAxes) {
        public static ExcludedItem from(FlavorEvaluation evaluation) {
            return new ExcludedItem(
                    evaluation.style().styleId(),
                    evaluation.style().name(),
                    evaluation.missingAxes());
        }
    }

    private static Map<FlavorAxis, List<String>> defaultedExternalIds(
            List<FlavorEvaluation> determined) {
        return determined.stream()
                .flatMap(
                        evaluation ->
                                evaluation.defaultedAxes().stream()
                                        .map(axis -> Map.entry(axis, evaluation.style().styleId())))
                .collect(
                        groupingBy(
                                Map.Entry::getKey,
                                () -> new EnumMap<>(FlavorAxis.class),
                                mapping(Map.Entry::getValue, toList())));
    }

    // 리포트에 축이 늘 같은 순서로 나오도록 enum 선언 순서를 유지한다.
    private static Map<FlavorAxis, List<String>> immutableEnumMap(
            Map<FlavorAxis, List<String>> externalIdsByAxis) {
        Map<FlavorAxis, List<String>> copy = new EnumMap<>(FlavorAxis.class);
        externalIdsByAxis.forEach((axis, externalIds) -> copy.put(axis, List.copyOf(externalIds)));
        return Collections.unmodifiableMap(copy);
    }
}
