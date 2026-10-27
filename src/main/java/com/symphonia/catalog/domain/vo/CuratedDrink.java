package com.symphonia.catalog.domain.vo;

import com.symphonia.catalog.domain.entity.Drink;
import com.symphonia.catalog.domain.entity.DrinkStyle;
import java.util.Set;

// 국내 유통 맥주처럼 사람이 매핑표로 직접 고른 제품이다. 스타일은 BJCP 스타일 id로 가리킨다.
public record CuratedDrink(String externalId, String name, double abv, String bjcpStyleId) {
    // 제품 맛은 매핑한 스타일의 기본 맛을, 음식 계열은 그 스타일의 음식 계열을 그대로 쓴다.
    public Drink toDrink(DrinkStyle drinkStyle, Set<Harmonize> harmonizes) {
        return Drink.of(
                drinkStyle.getId(),
                DrinkSource.CURATED,
                externalId,
                name,
                abv,
                drinkStyle.getFlavorProfile(),
                false,
                harmonizes);
    }
}
