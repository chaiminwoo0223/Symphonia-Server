package com.symphonia.gathering.presentation.dto.request;

import com.symphonia.gathering.application.dto.command.RecoverParticipantLinkCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RecoverParticipantLinkRequest(
        @NotBlank @Schema(description = "구글 로그인으로 받은 ID 토큰. 본인 확인에만 쓰고 저장하지 않는다")
                String googleIdToken) {
    public RecoverParticipantLinkCommand toCommand(String joinToken) {
        return new RecoverParticipantLinkCommand(joinToken, googleIdToken);
    }
}
