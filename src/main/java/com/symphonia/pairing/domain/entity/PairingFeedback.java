package com.symphonia.pairing.domain.entity;

import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.PairingFeedbackChange;
import com.symphonia.pairing.domain.vo.PairingRating;
import com.symphonia.pairing.domain.vo.PairingSituation;
import com.symphonia.pairing.domain.vo.RelationshipType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PairingFeedback {
    private Long id;
    private Long memberId;
    private Long drinkId;
    private Long anjuId;
    private RelationshipType relationshipType;
    private MoodType moodType;
    private PairingRating rating;

    public static PairingFeedback of(
            Long memberId,
            Long drinkId,
            Long anjuId,
            RelationshipType relationshipType,
            MoodType moodType,
            PairingRating rating) {
        return PairingFeedback.builder()
                .memberId(memberId)
                .drinkId(drinkId)
                .anjuId(anjuId)
                .relationshipType(relationshipType)
                .moodType(moodType)
                .rating(rating)
                .build();
    }

    // 회원이 아닌 사람의 피드백이다. 모임 데이터와 회원 데이터를 피드백으로 연결하지 않도록 회원 id를 남기지 않는다(#131).
    public static PairingFeedback anonymous(
            Long drinkId,
            Long anjuId,
            RelationshipType relationshipType,
            MoodType moodType,
            PairingRating rating) {
        return of(null, drinkId, anjuId, relationshipType, moodType, rating);
    }

    // 처음 낸 피드백이 조합별 집계에 더하는 수다.
    public PairingFeedbackChange addedChange() {
        return PairingFeedbackChange.added(situation(), rating);
    }

    // 같은 조합에 다시 낸 피드백으로 바꾸고, 전역 집계와 상황별 집계가 바뀌는 수를 돌려준다.
    public PairingFeedbackChange resubmit(
            RelationshipType relationshipType, MoodType moodType, PairingRating rating) {
        PairingSituation resubmittedSituation = PairingSituation.of(relationshipType, moodType);
        PairingFeedbackChange change =
                PairingFeedbackChange.between(
                        situation(), this.rating, resubmittedSituation, rating);
        this.relationshipType = relationshipType;
        this.moodType = moodType;
        this.rating = rating;

        return change;
    }

    public PairingSituation situation() {
        return PairingSituation.of(relationshipType, moodType);
    }
}
