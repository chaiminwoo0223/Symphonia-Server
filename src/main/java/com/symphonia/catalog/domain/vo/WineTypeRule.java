package com.symphonia.catalog.domain.vo;

import com.symphonia.catalog.domain.entity.DrinkStyle;
import java.util.Set;

// X-Wines의 Type 하나를 와인 스타일 하나로 매핑한다. flavorProfile은 그 스타일의 기본 맛이다.
// allergyTypes는 그 Type 와인의 알레르기 유발 성분이다.
public record WineTypeRule(
        String type,
        String externalId,
        String name,
        FlavorProfile flavorProfile,
        Set<AllergyType> allergyTypes) {
    public WineTypeRule {
        allergyTypes = Set.copyOf(allergyTypes);
    }

    public boolean matches(XWine wine) {
        return type.equals(wine.type());
    }

    public DrinkStyle toDrinkStyle() {
        return DrinkStyle.of(
                DrinkStyleSource.X_WINES, externalId, name, DrinkCategory.WINE, flavorProfile);
    }
}
