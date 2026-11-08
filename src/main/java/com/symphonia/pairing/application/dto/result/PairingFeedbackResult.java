package com.symphonia.pairing.application.dto.result;

import com.symphonia.pairing.domain.entity.PairingFeedback;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.PairingRating;
import com.symphonia.pairing.domain.vo.RelationshipType;

// created는 이번 제출로 피드백이 새로 생겼는지다. 같은 조합에 다시 내서 기존 피드백을 바꿨으면 false다.
public record PairingFeedbackResult(
        Long id,
        Long memberId,
        Long drinkId,
        Long anjuId,
        RelationshipType relationshipType,
        MoodType moodType,
        PairingRating rating,
        boolean created) {
    public static PairingFeedbackResult submitted(PairingFeedback pairingFeedback) {
        return of(pairingFeedback, true);
    }

    public static PairingFeedbackResult resubmitted(PairingFeedback pairingFeedback) {
        return of(pairingFeedback, false);
    }

    private static PairingFeedbackResult of(PairingFeedback pairingFeedback, boolean created) {
        return new PairingFeedbackResult(
                pairingFeedback.getId(),
                pairingFeedback.getMemberId(),
                pairingFeedback.getDrinkId(),
                pairingFeedback.getAnjuId(),
                pairingFeedback.getRelationshipType(),
                pairingFeedback.getMoodType(),
                pairingFeedback.getRating(),
                created);
    }
}
