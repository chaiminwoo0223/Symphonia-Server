package com.symphonia.pairing.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Occasion {
    private RelationshipType relationshipType;
    private MoodType moodType;

    public static Occasion of(RelationshipType relationshipType, MoodType moodType) {
        return new Occasion(relationshipType, moodType);
    }

    public MoodProfile toMoodProfile() {
        return moodType.baseMoodProfile()
                .withFormality(relationshipType.formalityBoost())
                .withRomance(relationshipType.romanceBoost());
    }

    public double maxAbv() {
        return relationshipType.maxAbv();
    }

    public boolean prefersLightAnju() {
        return relationshipType.prefersLightAnju() || moodType == MoodType.FORMAL;
    }
}
