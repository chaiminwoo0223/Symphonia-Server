package com.symphonia.gathering.presentation.dto.request;

import com.symphonia.gathering.application.dto.command.JoinGatheringCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinGatheringRequest(
        @NotBlank @Size(max = 20) @Schema(description = "참여자 이름") String name,
        @NotBlank @Schema(description = "휴대폰 번호 (하이픈 포함 가능)") String phoneNumber,
        @Schema(description = "오늘 술 안 마심 여부") boolean nonDrinker,
        @Schema(description = "개인정보 수집 및 이용 동의 여부") boolean consentAgreed,
        @NotBlank @Schema(description = "참여 화면에서 받은 동의 안내 버전") String consentVersion) {
    public JoinGatheringCommand toCommand(String joinToken) {
        return new JoinGatheringCommand(
                joinToken, name, phoneNumber, nonDrinker, consentAgreed, consentVersion);
    }
}
