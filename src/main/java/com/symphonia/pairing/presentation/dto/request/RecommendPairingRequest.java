package com.symphonia.pairing.presentation.dto.request;

import com.symphonia.pairing.application.dto.query.RecommendPairingQuery;
import com.symphonia.pairing.domain.vo.AttendeeConstraint;
import com.symphonia.pairing.domain.vo.MoodType;
import com.symphonia.pairing.domain.vo.RelationshipType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;

public record RecommendPairingRequest(
        @NotNull @Schema(description = "관계 유형") RelationshipType relationshipType,
        @NotNull @Schema(description = "분위기 유형") MoodType moodType,
        @AssertTrue(message = "성인 인증이 필요합니다.") @Schema(description = "성인 인증 여부")
                boolean isAdultConfirmed,
        @Schema(description = "참석자 음주 제약(운전, 임신 등)") List<AttendeeConstraint> attendeeConstraints) {
    public RecommendPairingQuery toQuery() {
        return new RecommendPairingQuery(relationshipType, moodType, toSet(attendeeConstraints));
    }

    private static <T> Set<T> toSet(List<T> values) {
        return values == null ? Set.of() : Set.copyOf(values);
    }
}
