package com.symphonia.pairing.application.dto.command;

import com.symphonia.catalog.application.dto.query.CatalogReferencesQuery;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.PairingRating;
import com.symphonia.pairing.domain.vo.RelationshipType;

public record SubmitAnonymousPairingFeedbackCommand(
        Long drinkId,
        Long anjuId,
        RelationshipType relationshipType,
        MoodType moodType,
        PairingRating rating) {
    public static SubmitAnonymousPairingFeedbackCommand of(
            Long drinkId,
            Long anjuId,
            RelationshipType relationshipType,
            MoodType moodType,
            PairingRating rating) {
        return new SubmitAnonymousPairingFeedbackCommand(
                drinkId, anjuId, relationshipType, moodType, rating);
    }

    public CatalogReferencesQuery toCatalogReferencesQuery() {
        return CatalogReferencesQuery.of(drinkId, anjuId);
    }
}
