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
    FRIEND_FORMAL(1L, 1L, 1L, 1L, RelationshipType.FRIEND, MoodType.FORMAL, PairingRating.LIKE);

    private final Long id;
    private final Long memberId;
    private final Long drinkId;
    private final Long anjuId;
    private final RelationshipType relationshipType;
    private final MoodType moodType;
    private final PairingRating rating;

    public PairingFeedback create() {
        return build(id, memberId, drinkId, anjuId);
    }

    // FK가 걸린 drink, anju는 실제로 저장된 id를 받아 아직 저장 전인 피드백을 만든다.
    public PairingFeedback createWithReferences(Long drinkId, Long anjuId) {
        return build(null, memberId, drinkId, anjuId);
    }

    // 실제로 저장된 회원이 낸 피드백이 필요할 때 회원 id도 함께 받는다.
    public PairingFeedback createWithMemberAndReferences(Long memberId, Long drinkId, Long anjuId) {
        return build(null, memberId, drinkId, anjuId);
    }

    // 정산 페이지에서 참여자가 낸 익명 피드백이다. 회원 id를 남기지 않는다(#131).
    public PairingFeedback createAnonymousWithReferences(Long drinkId, Long anjuId) {
        return build(null, null, drinkId, anjuId);
    }

    // 저장된 피드백을 흉내 낼 때 id와 참조 id를 함께 받는다.
    public PairingFeedback createWithIdAndReferences(Long id, Long drinkId, Long anjuId) {
        return build(id, memberId, drinkId, anjuId);
    }

    private PairingFeedback build(Long id, Long memberId, Long drinkId, Long anjuId) {
        return PairingFeedback.builder()
                .id(id)
                .memberId(memberId)
                .drinkId(drinkId)
                .anjuId(anjuId)
                .relationshipType(relationshipType)
                .moodType(moodType)
                .rating(rating)
                .build();
    }
}
