package com.symphonia.catalog.application.dto.result;

import com.symphonia.catalog.domain.vo.CuratedDrink;
import java.util.List;

public record ImportCuratedDrinkResult(int importedCount, List<UnmappedDrink> unmappedDrinks) {
    public ImportCuratedDrinkResult {
        unmappedDrinks = List.copyOf(unmappedDrinks);
    }

    // 매핑표의 전체 행 중 스타일이나 스타일의 음식 계열 규칙을 찾지 못한 행을 뺀 나머지가 적재된 제품이다.
    public static ImportCuratedDrinkResult of(int totalCount, List<UnmappedDrink> unmappedDrinks) {
        return new ImportCuratedDrinkResult(totalCount - unmappedDrinks.size(), unmappedDrinks);
    }

    public record UnmappedDrink(String externalId, String name, String bjcpStyleId) {
        public static UnmappedDrink from(CuratedDrink curatedDrink) {
            return new UnmappedDrink(
                    curatedDrink.externalId(), curatedDrink.name(), curatedDrink.bjcpStyleId());
        }
    }
}
