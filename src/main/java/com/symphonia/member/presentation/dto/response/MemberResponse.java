package com.symphonia.member.presentation.dto.response;

import com.symphonia.member.application.dto.result.MemberResult;

public record MemberResponse(
        Long id,
        String nickname,
        String email,
        String profileImage,
        String role,
        String socialProvider
) {
    public static MemberResponse from(MemberResult result) {
        return new MemberResponse(
                result.id(),
                result.nickname(),
                result.email(),
                result.profileImage(),
                result.role().name(),
                result.socialProvider().name()
        );
    }
}
