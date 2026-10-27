package com.symphonia.pairing.application.dto.command;

import com.symphonia.catalog.application.dto.query.VerifyCatalogReferencesQuery;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.PairingRating;
import com.symphonia.pairing.domain.vo.RelationshipType;

public record SubmitPairingFeedbackCommand(
        Long memberId,
        Long drinkId,
        Long anjuId,
        Long musicMoodId,
        RelationshipType relationshipType,
        MoodType moodType,
        PairingRating rating) {
    public VerifyCatalogReferencesQuery toVerifyCatalogReferencesQuery() {
        return VerifyCatalogReferencesQuery.of(drinkId, anjuId, musicMoodId);
    }
}
