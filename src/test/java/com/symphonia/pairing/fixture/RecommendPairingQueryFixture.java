package com.symphonia.pairing.fixture;

import com.symphonia.catalog.domain.vo.AllergyType;
import com.symphonia.pairing.application.dto.query.RecommendPairingQuery;
import com.symphonia.pairing.domain.vo.AttendeeConstraint;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.RelationshipType;
import java.util.Set;

public class RecommendPairingQueryFixture {
    private RelationshipType relationshipType = RelationshipType.FRIEND;
    private MoodType moodType = MoodType.CASUAL;
    private Set<AttendeeConstraint> attendeeConstraints = Set.of();
    private Set<AllergyType> attendeeAllergies = Set.of();

    public RecommendPairingQueryFixture relationshipType(RelationshipType relationshipType) {
        this.relationshipType = relationshipType;
        return this;
    }

    public RecommendPairingQueryFixture moodType(MoodType moodType) {
        this.moodType = moodType;
        return this;
    }

    public RecommendPairingQueryFixture attendeeConstraints(
            AttendeeConstraint... attendeeConstraints) {
        this.attendeeConstraints = Set.of(attendeeConstraints);
        return this;
    }

    public RecommendPairingQueryFixture attendeeAllergies(AllergyType... attendeeAllergies) {
        this.attendeeAllergies = Set.of(attendeeAllergies);
        return this;
    }

    public RecommendPairingQuery build() {
        return new RecommendPairingQuery(
                relationshipType, moodType, attendeeConstraints, attendeeAllergies);
    }
}
