package com.symphonia.catalog.domain.vo;

import com.symphonia.catalog.domain.entity.Anju;
import java.util.Collection;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;

// X-Wines Harmonize 상위 20개 태그다. 태그마다 안주 속성 하나만 보고 맞는지 판정하며, 판정 결과는 저장하지 않는다.
// 맞는 한국 안주가 없는 Veal은 BEEF로 흡수하고, Game Meat은 오리를 포함한 GAME 계열로 판정한다.
@RequiredArgsConstructor
public enum Harmonize {
    POULTRY(base(AnjuBase.POULTRY)),
    BEEF(base(AnjuBase.BEEF)),
    LAMB(base(AnjuBase.LAMB)),
    GAME_MEAT(base(AnjuBase.GAME)),
    PORK(base(AnjuBase.PORK)),
    SHELLFISH(base(AnjuBase.SEAFOOD)),
    VEAL(base(AnjuBase.BEEF)),
    VEGETARIAN(base(AnjuBase.VEGETABLE)),
    RICH_FISH(base(AnjuBase.RICH_FISH)),
    PASTA(base(AnjuBase.NOODLE)),
    SPICY_FOOD(Anju::isSpicy),
    MATURATED_CHEESE(base(AnjuBase.MATURATED_CHEESE)),
    HARD_CHEESE(base(AnjuBase.HARD_CHEESE)),
    CURED_MEAT(base(AnjuBase.CURED_MEAT)),
    APPETIZER(serving(ServingStyle.APPETIZER)),
    SNACK(serving(ServingStyle.SNACK)),
    SOFT_CHEESE(base(AnjuBase.SOFT_CHEESE)),
    LEAN_FISH(base(AnjuBase.LEAN_FISH)),
    SWEET_DESSERT(base(AnjuBase.SWEET)),
    GOAT_CHEESE(base(AnjuBase.GOAT_CHEESE));

    private final Predicate<Anju> condition;

    public boolean matches(Anju anju) {
        return condition.test(anju);
    }

    // 추천은 요청마다 음료×안주 쌍 전체에서 음식 계열이 겹치는지 본다. 그래서 계열 집합을 비트 하나씩으로 나타내
    // 쌍마다 비트 AND 한 번으로 판정하게 한다. 계열은 20개라 long에 들어간다.
    public static long maskOf(Collection<Harmonize> harmonizes) {
        long mask = 0;
        for (Harmonize harmonize : harmonizes) {
            mask |= harmonize.bit();
        }
        return mask;
    }

    public static long maskMatching(Anju anju) {
        long mask = 0;
        for (Harmonize harmonize : values()) {
            if (harmonize.matches(anju)) {
                mask |= harmonize.bit();
            }
        }
        return mask;
    }

    private static Predicate<Anju> base(AnjuBase base) {
        return anju -> anju.getBase() == base;
    }

    private static Predicate<Anju> serving(ServingStyle serving) {
        return anju -> anju.getServing() == serving;
    }

    private long bit() {
        return 1L << ordinal();
    }
}
