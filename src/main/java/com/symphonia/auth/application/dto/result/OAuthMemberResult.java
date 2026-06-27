package com.symphonia.auth.application.dto.result;

import com.symphonia.member.domain.entity.SocialProvider;

public record OAuthMemberResult(
        String socialId,
        String nickname,
        String email,
        String profileImage,
        SocialProvider socialProvider
) {
}
