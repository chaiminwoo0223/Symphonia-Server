package com.symphonia.pairing.domain.vo;

import com.symphonia.pairing.domain.entity.Anju;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Occasion {
    private RelationshipType relationshipType;
    private MoodType moodType;
    private Set<AttendeeConstraint> attendeeConstraints;
    private Set<AllergyType> attendeeAllergies;

    public static Occasion of(
            RelationshipType relationshipType,
            MoodType moodType,
            Set<AttendeeConstraint> attendeeConstraints,
            Set<AllergyType> attendeeAllergies) {
        return new Occasion(
                relationshipType,
                moodType,
                attendeeConstraints == null ? Set.of() : attendeeConstraints,
                attendeeAllergies == null ? Set.of() : attendeeAllergies);
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

    public boolean requiresNonAlcoholicOption() {
        return attendeeConstraints.contains(AttendeeConstraint.DRIVER)
                || attendeeConstraints.contains(AttendeeConstraint.PREGNANT)
                || attendeeConstraints.contains(AttendeeConstraint.NON_DRINKER);
    }

    public boolean hasAllergyConflict(Anju anju) {
        return anju.conflictsWith(attendeeAllergies);
    }
}
