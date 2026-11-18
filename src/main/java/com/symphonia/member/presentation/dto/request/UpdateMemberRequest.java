package com.symphonia.member.presentation.dto.request;

import com.symphonia.member.application.dto.command.UpdateMemberCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMemberRequest(
        @NotBlank @Size(max = 20) @Schema(description = "닉네임 (최대 20자)") String nickname) {
    public UpdateMemberCommand toCommand() {
        return new UpdateMemberCommand(nickname);
    }
}
