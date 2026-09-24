package com.symphonia.pairing.fixture;

import com.symphonia.pairing.domain.entity.PairingFeedback;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.PairingRating;
import com.symphonia.pairing.domain.vo.RelationshipType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PairingFeedbackFixture {
    FRIEND_FORMAL(1L, 1L, 1L, 1L, 1L, RelationshipType.FRIEND, MoodType.FORMAL, PairingRating.LIKE);

    private final Long id;
    private final Long memberId;
    private final Long drinkId;
    private final Long anjuId;
    private final Long musicMoodId;
    private final RelationshipType relationshipType;
    private final MoodType moodType;
    private final PairingRating rating;

    public PairingFeedback create() {
        return PairingFeedback.builder()
                .id(id)
                .memberId(memberId)
                .drinkId(drinkId)
                .anjuId(anjuId)
                .musicMoodId(musicMoodId)
                .relationshipType(relationshipType)
                .moodType(moodType)
                .rating(rating)
                .build();
    }
}
