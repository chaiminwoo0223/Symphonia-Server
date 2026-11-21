package com.symphonia.gathering.presentation.dto.request;

import com.symphonia.gathering.application.dto.command.JoinGatheringAsHostCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinGatheringAsHostRequest(
        @NotBlank @Size(max = 20) @Schema(description = "참여자 이름") String name,
        @NotBlank
                @Schema(
                        description =
                                "휴대폰 번호. 010으로 시작하는 11자리이며 하이픈은 넣어도 됩니다. +82나 공백이 섞인 번호는 클라이언트가 정리해서 보냅니다")
                String phoneNumber,
        @Schema(description = "개인정보 수집 및 이용 동의 여부") boolean consentAgreed,
        @NotBlank @Schema(description = "기록 화면에서 받은 동의 안내 버전") String consentVersion) {
    public JoinGatheringAsHostCommand toCommand(Long memberId, Long gatheringId) {
        return new JoinGatheringAsHostCommand(
                memberId, gatheringId, name, phoneNumber, consentAgreed, consentVersion);
    }
}
