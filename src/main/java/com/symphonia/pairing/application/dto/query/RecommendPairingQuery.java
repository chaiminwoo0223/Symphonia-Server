package com.symphonia.pairing.application.dto.query;

import com.symphonia.catalog.domain.vo.AllergyType;
import com.symphonia.pairing.domain.vo.AttendeeConstraint;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.RelationshipType;
import java.util.Set;

public record RecommendPairingQuery(
        RelationshipType relationshipType,
        MoodType moodType,
        Set<AttendeeConstraint> attendeeConstraints,
        Set<AllergyType> attendeeAllergies,
        // 로그인하지 않은 요청이면 null이다.
        Long memberId) {}
