package com.symphonia.member.presentation.dto.request;

import com.symphonia.member.application.dto.command.UpdateMemberCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record UpdateMemberRequest(@NotBlank @Schema(description = "닉네임") String nickname) {
    public UpdateMemberCommand toCommand() {
        return new UpdateMemberCommand(nickname);
    }
}
