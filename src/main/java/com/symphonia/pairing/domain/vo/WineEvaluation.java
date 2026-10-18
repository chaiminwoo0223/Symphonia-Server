package com.symphonia.pairing.domain.vo;

import com.symphonia.pairing.domain.entity.Drink;
import com.symphonia.pairing.domain.entity.DrinkStyle;
import com.symphonia.pairing.domain.exception.UndeterminedEvaluationException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

// 평가한 와인을 함께 담는다.
// 맛을 정할 수 없으면 styleExternalId와 flavorProfile은 null이다. 이때 unknownAttributes에는 규칙표에 없는 속성이 담긴다.
public record WineEvaluation(
        XWine wine,
        String styleExternalId,
        FlavorProfile flavorProfile,
        Set<WineAttribute> unknownAttributes) {
    public WineEvaluation {
        unknownAttributes = immutableEnumSet(unknownAttributes);
    }

    public static WineEvaluation determined(
            XWine wine, String styleExternalId, FlavorProfile flavorProfile) {
        return new WineEvaluation(wine, styleExternalId, flavorProfile, Set.of());
    }

    public static WineEvaluation undetermined(XWine wine, Set<WineAttribute> unknownAttributes) {
        return new WineEvaluation(wine, null, null, unknownAttributes);
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
                false);
    }

    // 리포트에 속성이 늘 같은 순서로 나오도록 enum 선언 순서를 유지한다.
    private static Set<WineAttribute> immutableEnumSet(Set<WineAttribute> attributes) {
        EnumSet<WineAttribute> copy = EnumSet.noneOf(WineAttribute.class);
        copy.addAll(attributes);
        return Collections.unmodifiableSet(copy);
    }
}
