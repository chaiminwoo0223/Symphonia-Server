package com.symphonia.pairing.application.dto.result;

import com.symphonia.pairing.domain.entity.PairingFeedback;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.RelationshipType;

public record PairingFeedbackResult(
        Long id,
        Long memberId,
        Long drinkId,
        Long anjuId,
        Long musicMoodId,
        RelationshipType relationshipType,
        MoodType moodType) {
    public static PairingFeedbackResult from(PairingFeedback pairingFeedback) {
        return new PairingFeedbackResult(
                pairingFeedback.getId(),
                pairingFeedback.getMemberId(),
                pairingFeedback.getDrinkId(),
                pairingFeedback.getAnjuId(),
                pairingFeedback.getMusicMoodId(),
                pairingFeedback.getRelationshipType(),
                pairingFeedback.getMoodType());
    }
}
