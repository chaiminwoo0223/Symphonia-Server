package com.symphonia.pairing.application.dto.command;

import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.PairingSituation;
import com.symphonia.pairing.domain.vo.RelationshipType;

public record RecordPairingSelectionCommand(
        Long drinkId, Long anjuId, RelationshipType relationshipType, MoodType moodType) {
    public static RecordPairingSelectionCommand of(
            Long drinkId, Long anjuId, RelationshipType relationshipType, MoodType moodType) {
        return new RecordPairingSelectionCommand(drinkId, anjuId, relationshipType, moodType);
    }

    public PairingSituation toSituation() {
        return PairingSituation.of(relationshipType, moodType);
    }
}
