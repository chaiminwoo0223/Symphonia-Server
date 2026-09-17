package com.symphonia.pairing.application.dto.query;

import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.RelationshipType;

public record RecommendPairingQuery(RelationshipType relationshipType, MoodType moodType) {}
