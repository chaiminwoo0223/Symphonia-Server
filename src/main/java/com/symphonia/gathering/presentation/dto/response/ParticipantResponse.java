package com.symphonia.gathering.presentation.dto.response;

import com.symphonia.gathering.application.dto.result.ParticipantResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record ParticipantResponse(
        @Schema(description = "참여자 ID") Long id,
        @Schema(description = "참여자 이름") String name,
        @Schema(description = "마스킹한 휴대폰 번호") String maskedPhoneNumber,
        @Schema(description = "오늘 술 안 마심 여부") boolean nonDrinker,
        @Schema(description = "개인 링크 토큰 (이 응답에서만 받을 수 있음)") String linkToken) {
    public static ParticipantResponse from(ParticipantResult result) {
        return new ParticipantResponse(
                result.id(),
                result.name(),
                result.maskedPhoneNumber(),
                result.nonDrinker(),
                result.linkToken());
    }
}
