package com.symphonia.pairing.domain.vo;

import com.symphonia.pairing.domain.entity.Anju;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

// 평가한 매핑표 행을 함께 담는다.
// 적재할 수 없으면 anju는 null이다. 이때 exclusionReasons에 이유가, unknownIngredients에 규칙표에 없는 재료가 담긴다.
public record AnjuEvaluation(
        CuratedAnju curatedAnju,
        Anju anju,
        Set<AnjuExclusionReason> exclusionReasons,
        Set<String> unknownIngredients) {
    public AnjuEvaluation {
        EnumSet<AnjuExclusionReason> reasons = EnumSet.noneOf(AnjuExclusionReason.class);
        reasons.addAll(exclusionReasons);
        exclusionReasons = Collections.unmodifiableSet(reasons);
        // 리포트에 재료가 늘 같은 순서로 나오도록 이름순으로 정렬한다.
        unknownIngredients = Collections.unmodifiableSet(new TreeSet<>(unknownIngredients));
    }

    public static AnjuEvaluation determined(CuratedAnju curatedAnju, Anju anju) {
        return new AnjuEvaluation(curatedAnju, anju, Set.of(), Set.of());
    }

    public static AnjuEvaluation excluded(
            CuratedAnju curatedAnju,
            Set<AnjuExclusionReason> exclusionReasons,
            Set<String> unknownIngredients) {
        return new AnjuEvaluation(curatedAnju, null, exclusionReasons, unknownIngredients);
    }

    public boolean isDetermined() {
        return exclusionReasons.isEmpty();
    }

    public Anju toAnju() {
        return Objects.requireNonNull(anju, "적재할 수 없는 안주는 Anju로 바꿀 수 없습니다.");
    }
}
