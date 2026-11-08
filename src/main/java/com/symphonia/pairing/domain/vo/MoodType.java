package com.symphonia.pairing.domain.vo;

import com.symphonia.catalog.domain.vo.AnjuBase;
import com.symphonia.catalog.domain.vo.DrinkCategory;
import com.symphonia.catalog.domain.vo.ServingStyle;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;

// 분위기마다 어울리는 음료와 안주의 조건이다. 후보를 거르지 않고 가산점과 추천 사유에만 쓴다.
@RequiredArgsConstructor
public enum MoodType {
    FORMAL(
            drink -> drink.isCategoryOf(Preference.FORMAL_DRINKS),
            anju ->
                    (anju.isServedAs(ServingStyle.APPETIZER)
                                    || anju.isBasedOnAnyOf(Preference.CHEESES))
                            && !anju.isSpicy()),
    CASUAL(
            drink -> drink.isCategoryOf(Preference.CASUAL_DRINKS),
            anju ->
                    anju.isServedAs(ServingStyle.SNACK)
                            || anju.isBasedOnAnyOf(Preference.CASUAL_ANJU_BASES)),
    ROMANTIC(
            drink -> drink.isCategoryOf(Preference.ROMANTIC_DRINKS),
            anju ->
                    anju.isServedAs(ServingStyle.APPETIZER)
                            || anju.isBasedOnAnyOf(Preference.ROMANTIC_ANJU_BASES)),
    CELEBRATORY(PairingDrink::isSparkling, anju -> anju.isServedAs(ServingStyle.MAIN)),
    ;

    private final Predicate<PairingDrink> drinkPreference;
    private final Predicate<PairingAnju> anjuPreference;

    public boolean prefers(PairingDrink drink) {
        return drinkPreference.test(drink);
    }

    public boolean prefers(PairingAnju anju) {
        return anjuPreference.test(anju);
    }

    // 쌍마다 판정하므로 조건 집합을 요청마다 만들지 않고 한 번만 만든다.
    private static final class Preference {
        private static final Set<DrinkCategory> FORMAL_DRINKS =
                EnumSet.of(DrinkCategory.WINE, DrinkCategory.WHISKY);
        private static final Set<DrinkCategory> CASUAL_DRINKS =
                EnumSet.of(
                        DrinkCategory.BEER,
                        DrinkCategory.SOJU,
                        DrinkCategory.MAKGEOLLI,
                        DrinkCategory.HIGHBALL);
        private static final Set<DrinkCategory> ROMANTIC_DRINKS = EnumSet.of(DrinkCategory.WINE);
        private static final Set<AnjuBase> CHEESES =
                EnumSet.of(
                        AnjuBase.HARD_CHEESE,
                        AnjuBase.MATURATED_CHEESE,
                        AnjuBase.SOFT_CHEESE,
                        AnjuBase.GOAT_CHEESE);
        private static final Set<AnjuBase> CASUAL_ANJU_BASES =
                EnumSet.of(AnjuBase.POULTRY, AnjuBase.PORK);
        private static final Set<AnjuBase> ROMANTIC_ANJU_BASES =
                EnumSet.of(
                        AnjuBase.HARD_CHEESE,
                        AnjuBase.MATURATED_CHEESE,
                        AnjuBase.SOFT_CHEESE,
                        AnjuBase.GOAT_CHEESE,
                        AnjuBase.SWEET);
    }
}
