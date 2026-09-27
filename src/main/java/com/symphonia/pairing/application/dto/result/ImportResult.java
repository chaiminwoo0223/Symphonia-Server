package com.symphonia.pairing.application.dto.result;

import com.symphonia.pairing.domain.vo.FlavorAxis;
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

    public record ExcludedItem(String externalId, String name, Set<FlavorAxis> missingAxes) {}

    // 리포트에 축이 늘 같은 순서로 나오도록 enum 선언 순서를 유지한다.
    private static Map<FlavorAxis, List<String>> immutableEnumMap(
            Map<FlavorAxis, List<String>> externalIdsByAxis) {
        Map<FlavorAxis, List<String>> copy = new EnumMap<>(FlavorAxis.class);
        externalIdsByAxis.forEach((axis, externalIds) -> copy.put(axis, List.copyOf(externalIds)));
        return Collections.unmodifiableMap(copy);
    }
}
