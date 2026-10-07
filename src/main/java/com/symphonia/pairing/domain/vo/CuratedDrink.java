package com.symphonia.pairing.domain.vo;

import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.entity.DrinkStyle;

// 국내 유통 맥주처럼 사람이 매핑표로 직접 고른 제품이다. 스타일은 BJCP 스타일 id로 가리킨다.
public record CuratedDrink(String externalId, String name, double abv, String bjcpStyleId) {
    // 제품 맛은 매핑한 스타일의 기본 맛을 그대로 쓴다.
    public Drink toDrink(DrinkStyle drinkStyle) {
        return Drink.of(
                drinkStyle.getId(),
                DrinkSource.CURATED,
                externalId,
                name,
                abv,
                drinkStyle.getFlavorProfile(),
                false);
    }
}
