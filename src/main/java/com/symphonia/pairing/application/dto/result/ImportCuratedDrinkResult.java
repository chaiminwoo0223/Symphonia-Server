package com.symphonia.pairing.application.dto.result;

import com.symphonia.pairing.domain.vo.CuratedDrink;
import java.util.List;

public record ImportCuratedDrinkResult(int importedCount, List<UnmappedItem> unmappedItems) {
    public ImportCuratedDrinkResult {
        unmappedItems = List.copyOf(unmappedItems);
    }

    // 매핑표의 전체 행 중 스타일을 찾지 못한 행을 뺀 나머지가 적재된 제품이다.
    public static ImportCuratedDrinkResult of(int totalCount, List<UnmappedItem> unmappedItems) {
        return new ImportCuratedDrinkResult(totalCount - unmappedItems.size(), unmappedItems);
    }

    public record UnmappedItem(String externalId, String name, String bjcpStyleId) {
        public static UnmappedItem from(CuratedDrink curatedDrink) {
            return new UnmappedItem(
                    curatedDrink.externalId(), curatedDrink.name(), curatedDrink.bjcpStyleId());
        }
    }
}
