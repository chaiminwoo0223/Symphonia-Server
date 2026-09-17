package com.symphonia.pairing.domain.entity;

import com.symphonia.pairing.domain.vo.MoodType;
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

    public static PairingFeedback of(
            Long memberId,
            Long drinkId,
            Long anjuId,
            Long musicMoodId,
            RelationshipType relationshipType,
            MoodType moodType) {
        return PairingFeedback.builder()
                .memberId(memberId)
                .drinkId(drinkId)
                .anjuId(anjuId)
                .musicMoodId(musicMoodId)
                .relationshipType(relationshipType)
                .moodType(moodType)
                .build();
    }
}
