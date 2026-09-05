package com.symphonia.member.presentation.dto.response;

import com.symphonia.member.application.dto.result.MemberResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record MemberUpdateResponse(@Schema(description = "닉네임") String nickname) {
    public static MemberUpdateResponse from(MemberResult result) {
        return new MemberUpdateResponse(result.nickname());
    }
}
