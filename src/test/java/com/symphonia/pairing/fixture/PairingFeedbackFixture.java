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
        return build(id, memberId, drinkId, anjuId, musicMoodId);
    }

    // FK가 걸린 drink, anju, music mood는 실제로 저장된 id를 받아 아직 저장 전인 피드백을 만든다.
    public PairingFeedback createWithReferences(Long drinkId, Long anjuId, Long musicMoodId) {
        return build(null, memberId, drinkId, anjuId, musicMoodId);
    }

    // 실제로 저장된 회원이 낸 피드백이 필요할 때 회원 id도 함께 받는다.
    public PairingFeedback createWithMemberAndReferences(
            Long memberId, Long drinkId, Long anjuId, Long musicMoodId) {
        return build(null, memberId, drinkId, anjuId, musicMoodId);
    }

    // 저장된 피드백을 흉내 낼 때 id와 참조 id를 함께 받는다.
    public PairingFeedback createWithIdAndReferences(
            Long id, Long drinkId, Long anjuId, Long musicMoodId) {
        return build(id, memberId, drinkId, anjuId, musicMoodId);
    }

    private PairingFeedback build(
            Long id, Long memberId, Long drinkId, Long anjuId, Long musicMoodId) {
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
