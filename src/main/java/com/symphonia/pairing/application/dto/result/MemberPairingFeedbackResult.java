package com.symphonia.pairing.application.dto.result;

import com.symphonia.catalog.application.dto.result.CatalogReferencesResult;
import com.symphonia.pairing.domain.entity.PairingFeedback;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.PairingRating;
import com.symphonia.pairing.domain.vo.RelationshipType;
import java.time.LocalDateTime;

// 회원이 낸 피드백 하나다. 술과 안주의 이름은 읽을 때 catalog에서 가져온 값이다.
public record MemberPairingFeedbackResult(
        Long drinkId,
        String drinkName,
        Long anjuId,
        String anjuName,
        PairingRating rating,
        RelationshipType relationshipType,
        MoodType moodType,
        LocalDateTime submittedAt) {
    public static MemberPairingFeedbackResult of(
            PairingFeedback pairingFeedback, CatalogReferencesResult catalogReferences) {
        return new MemberPairingFeedbackResult(
                pairingFeedback.getDrinkId(),
                catalogReferences.drinkName(),
                pairingFeedback.getAnjuId(),
                catalogReferences.anjuName(),
                pairingFeedback.getRating(),
                pairingFeedback.getRelationshipType(),
                pairingFeedback.getMoodType(),
                pairingFeedback.getSubmittedAt());
    }
}
