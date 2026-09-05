package com.symphonia.member.presentation.dto.response;

import com.symphonia.member.application.dto.result.MemberResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record MemberResponse(
        @Schema(description = "ID") Long id,
        @Schema(description = "닉네임") String nickname,
        @Schema(description = "이메일") String email,
        @Schema(description = "프로필 이미지") String profileImage,
        @Schema(description = "역할") String role,
        @Schema(description = "소셜 로그인 제공자") String socialProvider) {
    public static MemberResponse from(MemberResult result) {
        return new MemberResponse(
                result.id(),
                result.nickname(),
                result.email(),
                result.profileImage(),
                result.role().name(),
                result.socialProvider().name());
    }
}
