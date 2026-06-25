package com.symphonia.member.presentation.dto.response;

import com.symphonia.member.application.dto.result.MemberResult;

public record MemberUpdateResponse(
        String nickname
) {
    public static MemberUpdateResponse from(MemberResult result) {
        return new MemberUpdateResponse(result.nickname());
    }
}
