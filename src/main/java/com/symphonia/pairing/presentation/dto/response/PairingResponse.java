package com.symphonia.pairing.presentation.dto.response;

import com.symphonia.pairing.application.dto.result.PairingResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record PairingResponse(
        @Schema(description = "술 ID") Long drinkId,
        @Schema(description = "술 이름") String drinkName,
        @Schema(description = "무알코올 여부") boolean drinkNonAlcoholic,
        @Schema(description = "안주 ID") Long anjuId,
        @Schema(description = "안주 이름") String anjuName,
        @Schema(description = "음악 무드 ID") Long musicMoodId,
        @Schema(description = "음악 무드 제목") String musicMoodTitle,
        @Schema(description = "스트리밍 링크") String streamingUrl,
        @Schema(description = "추천 점수") double score) {
    public static PairingResponse from(PairingResult result) {
        return new PairingResponse(
                result.drinkId(),
                result.drinkName(),
                result.drinkNonAlcoholic(),
                result.anjuId(),
                result.anjuName(),
                result.musicMoodId(),
                result.musicMoodTitle(),
                result.streamingUrl(),
                result.score());
    }
}
