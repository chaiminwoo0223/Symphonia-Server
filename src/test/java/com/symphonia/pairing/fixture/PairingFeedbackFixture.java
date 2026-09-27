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
        return build(id, drinkId, anjuId, musicMoodId);
    }

    // FK가 걸린 drink, anju, music mood는 실제로 저장된 id를 받아 아직 저장 전인 피드백을 만든다.
    public PairingFeedback createWithReferences(Long drinkId, Long anjuId, Long musicMoodId) {
        return build(null, drinkId, anjuId, musicMoodId);
    }

    private PairingFeedback build(Long id, Long drinkId, Long anjuId, Long musicMoodId) {
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
