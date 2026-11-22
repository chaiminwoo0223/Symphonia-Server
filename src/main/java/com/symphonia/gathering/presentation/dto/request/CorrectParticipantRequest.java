package com.symphonia.gathering.presentation.dto.request;

import com.symphonia.gathering.application.dto.command.CorrectParticipantCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CorrectParticipantRequest(
        @NotBlank @Schema(description = "구글 로그인으로 받은 ID 토큰. 본인 확인에만 쓰고 저장하지 않는다")
                String googleIdToken,
        @NotBlank @Size(max = 20) @Schema(description = "정정할 참여자 이름") String name,
        @NotBlank
                @Schema(
                        description =
                                "정정할 휴대폰 번호. 010으로 시작하는 11자리이며 하이픈은 넣어도 됩니다. +82나 공백이 섞인 번호는 클라이언트가 정리해서 보냅니다")
                String phoneNumber) {
    public CorrectParticipantCommand toCommand(String joinToken) {
        return new CorrectParticipantCommand(joinToken, googleIdToken, name, phoneNumber);
    }
}
