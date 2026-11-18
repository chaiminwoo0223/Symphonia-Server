package com.symphonia.gathering.presentation.dto.response;

import com.symphonia.gathering.application.dto.result.ParticipantPairingResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record ParticipantPairingResponse(
        @Schema(description = "술 ID. 피드백을 낼 때 그대로 되돌려 보냅니다.") Long drinkId,
        @Schema(description = "술 이름") String drinkName,
        @Schema(description = "안주 ID. 피드백을 낼 때 그대로 되돌려 보내고, 이 안주를 곁들일 맛집을 조회할 때도 씁니다.") Long anjuId,
        @Schema(description = "안주 이름") String anjuName,
        @Schema(description = "이미 피드백을 남겼는지 여부 (true면 피드백을 묻지 않음)") boolean feedbackSubmitted,
        @Schema(description = "지금 피드백을 낼 수 있는지 여부. 분담금을 보내거나 받은 참여자이고 아직 내지 않았을 때만 true입니다.")
                boolean feedbackSubmittable) {
    public static ParticipantPairingResponse from(ParticipantPairingResult result) {
        return new ParticipantPairingResponse(
                result.drinkId(),
                result.drinkName(),
                result.anjuId(),
                result.anjuName(),
                result.feedbackSubmitted(),
                result.feedbackSubmittable());
    }
}
