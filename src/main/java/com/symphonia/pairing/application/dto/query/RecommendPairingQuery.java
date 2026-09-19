package com.symphonia.pairing.application.dto.query;

import com.symphonia.pairing.domain.vo.AllergyType;
import com.symphonia.pairing.domain.vo.AttendeeConstraint;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.RelationshipType;
import java.util.Set;

public record RecommendPairingQuery(
        RelationshipType relationshipType,
        MoodType moodType,
        Set<AttendeeConstraint> attendeeConstraints,
        Set<AllergyType> attendeeAllergies) {}
