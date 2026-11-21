package com.symphonia.pairing.presentation.dto.response;

import com.symphonia.pairing.application.dto.result.MemberPairingFeedbackResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record MemberPairingFeedbackResponse(
        @Schema(description = "술 ID") Long drinkId,
        @Schema(description = "술 이름") String drinkName,
        @Schema(description = "안주 ID") Long anjuId,
        @Schema(description = "안주 이름") String anjuName,
        @Schema(description = "평가값") String rating,
        @Schema(description = "관계 유형") String relationshipType,
        @Schema(description = "분위기 유형. 분위기 없이 추천받은 조합이면 null") String moodType,
        @Schema(description = "처음 피드백을 낸 시각. 평가값을 바꿔도 변하지 않습니다.") LocalDateTime submittedAt) {
    public static MemberPairingFeedbackResponse from(MemberPairingFeedbackResult result) {
        return new MemberPairingFeedbackResponse(
                result.drinkId(),
                result.drinkName(),
                result.anjuId(),
                result.anjuName(),
                result.rating().name(),
                result.relationshipType().name(),
                result.moodType() == null ? null : result.moodType().name(),
                result.submittedAt());
    }
}
