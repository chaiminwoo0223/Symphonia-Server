package com.symphonia.auth.domain.identity;

public record SocialIdentity(
        String socialId,
        String nickname,
        String email,
        String profileImage,
        String socialProvider
) {
}
