package com.symphonia.pairing.domain.vo;

import com.symphonia.pairing.domain.entity.DrinkStyle;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

// 평가한 스타일을 함께 담는다.
// 맛을 정할 수 없으면 flavorProfile은 null이다. 이때 missingAxes에는 규칙이 없는 필수 축이 담긴다.
public record FlavorEvaluation(
        BjcpStyle style,
        FlavorProfile flavorProfile,
        Set<FlavorAxis> missingAxes,
        Set<FlavorAxis> defaultedAxes) {
    public FlavorEvaluation {
        missingAxes = immutableEnumSet(missingAxes);
        defaultedAxes = immutableEnumSet(defaultedAxes);
    }

    public static FlavorEvaluation determined(
            BjcpStyle style, FlavorProfile flavorProfile, Set<FlavorAxis> defaultedAxes) {
        return new FlavorEvaluation(style, flavorProfile, Set.of(), defaultedAxes);
    }

    public static FlavorEvaluation undetermined(BjcpStyle style, Set<FlavorAxis> missingAxes) {
        return new FlavorEvaluation(style, null, missingAxes, Set.of());
    }

    public boolean isDetermined() {
        return missingAxes.isEmpty();
    }

    public DrinkStyle toDrinkStyle() {
        Objects.requireNonNull(flavorProfile, "맛을 정할 수 없는 스타일은 DrinkStyle로 바꿀 수 없습니다.");
        return style.toDrinkStyle(flavorProfile);
    }

    // 리포트에 축이 늘 같은 순서로 나오도록 enum 선언 순서를 유지한다.
    private static Set<FlavorAxis> immutableEnumSet(Set<FlavorAxis> axes) {
        EnumSet<FlavorAxis> copy = EnumSet.noneOf(FlavorAxis.class);
        copy.addAll(axes);
        return Collections.unmodifiableSet(copy);
    }
}
