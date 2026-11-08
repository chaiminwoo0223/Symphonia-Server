package com.symphonia.gathering.application.dto.command;

import com.symphonia.catalog.application.dto.query.CatalogReferencesQuery;
import com.symphonia.gathering.domain.vo.GatheringPairing;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.RelationshipType;

// 모임 열기와 페어링 고르기가 함께 받는 페어링 값이다.
public record GatheringPairingCommand(
        Long drinkId, Long anjuId, RelationshipType relationshipType, MoodType moodType) {
    public CatalogReferencesQuery toCatalogReferencesQuery() {
        return CatalogReferencesQuery.of(drinkId, anjuId);
    }

    public GatheringPairing toGatheringPairing() {
        return GatheringPairing.of(drinkId, anjuId, relationshipType, moodType);
    }
}
