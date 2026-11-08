package com.symphonia.pairing.domain.vo;

import com.symphonia.catalog.domain.vo.AllergyType;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
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
        return Occasion.builder()
                .relationshipType(relationshipType)
                .moodType(moodType)
                .attendeeConstraints(attendeeConstraints == null ? Set.of() : attendeeConstraints)
                .attendeeAllergies(attendeeAllergies == null ? Set.of() : attendeeAllergies)
                .build();
    }

    public double maxAbv() {
        return relationshipType.maxAbv();
    }

    public boolean hasAbvLimit() {
        return relationshipType.maxAbv() < Double.MAX_VALUE;
    }

    public boolean prefersLightAnju() {
        return relationshipType.prefersLightAnju();
    }

    // 분위기는 선택 입력이다. 고르지 않았으면 어떤 음료와 안주도 우대하지 않는다.
    public boolean moodPrefers(PairingDrink drink) {
        return moodType != null && moodType.prefers(drink);
    }

    public boolean moodPrefers(PairingAnju anju) {
        return moodType != null && moodType.prefers(anju);
    }

    public boolean requiresNonAlcoholicOption() {
        return attendeeConstraints.contains(AttendeeConstraint.DRIVER)
                || attendeeConstraints.contains(AttendeeConstraint.PREGNANT)
                || attendeeConstraints.contains(AttendeeConstraint.NON_DRINKER);
    }

    public boolean hasAllergyConflict(PairingAnju anju) {
        return anju.conflictsWith(attendeeAllergies);
    }

    public boolean hasAllergyInput() {
        return !attendeeAllergies.isEmpty();
    }
}
