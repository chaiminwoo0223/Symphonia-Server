package com.symphonia.gathering.presentation.dto.request;

import com.symphonia.gathering.application.dto.command.WithdrawParticipantCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record WithdrawParticipantRequest(
        @NotBlank @Schema(description = "구글 로그인으로 받은 ID 토큰. 본인 확인에만 쓰고 저장하지 않는다")
                String googleIdToken) {
    public WithdrawParticipantCommand toCommand(String joinToken) {
        return new WithdrawParticipantCommand(joinToken, googleIdToken);
    }
}
