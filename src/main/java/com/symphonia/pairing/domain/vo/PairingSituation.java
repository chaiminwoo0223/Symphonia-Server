package com.symphonia.pairing.domain.vo;

import java.util.Comparator;

// 추천을 요청하거나 피드백을 남긴 자리의 관계 유형과 분위기다. 분위기를 고르지 않았으면 null이고, 그것도 하나의 상황이다.
public record PairingSituation(RelationshipType relationshipType, MoodType moodType)
        implements Comparable<PairingSituation> {
    // 같은 두 상황은 어떤 요청에서든 같은 순서로 다뤄, 집계 행을 잠그는 순서가 요청마다 뒤집히지 않게 한다.
    private static final Comparator<PairingSituation> ORDER =
            Comparator.comparing(PairingSituation::relationshipType)
                    .thenComparing(
                            PairingSituation::moodType,
                            Comparator.nullsFirst(Comparator.naturalOrder()));

    public static PairingSituation of(RelationshipType relationshipType, MoodType moodType) {
        return new PairingSituation(relationshipType, moodType);
    }

    @Override
    public int compareTo(PairingSituation other) {
        return ORDER.compare(this, other);
    }
}
