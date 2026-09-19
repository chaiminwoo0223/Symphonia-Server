package com.symphonia.pairing.presentation.dto.request;

import com.symphonia.pairing.application.dto.query.RecommendPairingQuery;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.RelationshipType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record RecommendPairingRequest(
        @NotNull @Schema(description = "관계 유형") RelationshipType relationshipType,
        @NotNull @Schema(description = "분위기 유형") MoodType moodType,
        @AssertTrue(message = "성인 인증이 필요합니다.") @Schema(description = "성인 인증 여부")
                boolean isAdultConfirmed) {
    public RecommendPairingQuery toQuery() {
        return new RecommendPairingQuery(relationshipType, moodType);
    }
}
