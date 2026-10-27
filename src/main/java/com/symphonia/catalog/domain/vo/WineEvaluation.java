package com.symphonia.catalog.domain.vo;

import com.symphonia.catalog.domain.entity.Drink;
import com.symphonia.catalog.domain.entity.DrinkStyle;
import com.symphonia.catalog.domain.exception.UndeterminedEvaluationException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

// 평가한 와인을 함께 담는다.
// 맛을 정할 수 없으면 styleExternalId와 flavorProfile은 null이고 harmonizes는 비어 있다.
// 이때 unknownAttributes에는 규칙표에 없는 속성이 담긴다.
public record WineEvaluation(
        XWine wine,
        String styleExternalId,
        FlavorProfile flavorProfile,
        Set<Harmonize> harmonizes,
        Set<WineAttribute> unknownAttributes) {
    public WineEvaluation {
        harmonizes = Set.copyOf(harmonizes);
        unknownAttributes = immutableEnumSet(unknownAttributes);
    }

    public static WineEvaluation determined(
            XWine wine,
            String styleExternalId,
            FlavorProfile flavorProfile,
            Set<Harmonize> harmonizes) {
        return new WineEvaluation(wine, styleExternalId, flavorProfile, harmonizes, Set.of());
    }

    public static WineEvaluation undetermined(XWine wine, Set<WineAttribute> unknownAttributes) {
        return new WineEvaluation(wine, null, null, Set.of(), unknownAttributes);
    }

    public boolean isDetermined() {
        return unknownAttributes.isEmpty();
    }

    public Drink toDrink(DrinkStyle drinkStyle) {
        if (!isDetermined()) {
            throw new UndeterminedEvaluationException();
        }
        return Drink.of(
                drinkStyle.getId(),
                DrinkSource.X_WINES,
                wine.wineId(),
                wine.displayName(),
                wine.abv(),
                flavorProfile,
                false,
                harmonizes);
    }

    // 리포트에 속성이 늘 같은 순서로 나오도록 enum 선언 순서를 유지한다.
    private static Set<WineAttribute> immutableEnumSet(Set<WineAttribute> attributes) {
        EnumSet<WineAttribute> copy = EnumSet.noneOf(WineAttribute.class);
        copy.addAll(attributes);
        return Collections.unmodifiableSet(copy);
    }
}
