package com.symphonia.pairing.domain.entity;

import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.PairingRating;
import com.symphonia.pairing.domain.vo.PairingRatingChange;
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
    private Long musicMoodId;
    private RelationshipType relationshipType;
    private MoodType moodType;
    private PairingRating rating;

    public static PairingFeedback of(
            Long memberId,
            Long drinkId,
            Long anjuId,
            Long musicMoodId,
            RelationshipType relationshipType,
            MoodType moodType,
            PairingRating rating) {
        return PairingFeedback.builder()
                .memberId(memberId)
                .drinkId(drinkId)
                .anjuId(anjuId)
                .musicMoodId(musicMoodId)
                .relationshipType(relationshipType)
                .moodType(moodType)
                .rating(rating)
                .build();
    }

    // 회원이 아닌 사람의 피드백이다. 모임 데이터와 회원 데이터를 피드백으로 연결하지 않도록 회원 id를 남기지 않는다(#131).
    public static PairingFeedback anonymous(
            Long drinkId,
            Long anjuId,
            Long musicMoodId,
            RelationshipType relationshipType,
            MoodType moodType,
            PairingRating rating) {
        return of(null, drinkId, anjuId, musicMoodId, relationshipType, moodType, rating);
    }

    // 같은 조합에 다시 낸 피드백으로 바꾸고, 조합별 집계가 바뀌는 수를 돌려준다.
    public PairingRatingChange resubmit(
            RelationshipType relationshipType, MoodType moodType, PairingRating rating) {
        PairingRatingChange change = PairingRatingChange.between(this.rating, rating);
        this.relationshipType = relationshipType;
        this.moodType = moodType;
        this.rating = rating;

        return change;
    }
}
