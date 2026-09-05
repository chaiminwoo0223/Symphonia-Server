package com.symphonia.member.presentation.dto.request;

import com.symphonia.member.application.dto.command.MemberUpdateCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record MemberUpdateRequest(@NotBlank @Schema(description = "닉네임") String nickname) {
  public MemberUpdateCommand toCommand() {
    return new MemberUpdateCommand(nickname);
  }
}
