package com.symphonia.pairing.presentation.dto.response;

import com.symphonia.pairing.application.dto.result.PairingRecommendationResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record PairingRecommendationResponse(
        @Schema(description = "추천한 술과 안주 조합. 추천할 조합이 없으면 빈 목록입니다.") List<PairingResponse> pairings,
        @Schema(description = "추천이 비었거나 무알코올 음료를 찾지 못한 사유. 해당하지 않으면 빈 목록입니다.")
                List<PairingNoticeResponse> notices) {
    public static PairingRecommendationResponse from(PairingRecommendationResult result) {
        return new PairingRecommendationResponse(
                result.pairings().stream().map(PairingResponse::from).toList(),
                result.notices().stream().map(PairingNoticeResponse::from).toList());
    }
}
