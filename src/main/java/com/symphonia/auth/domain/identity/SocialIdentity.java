package com.symphonia.auth.domain.identity;

import com.symphonia.auth.domain.exception.SocialRequiredInfoMissingException;

public record SocialIdentity(
        String socialId,
        String nickname,
        String email,
        String profileImage,
        String socialProvider) {

    public SocialIdentity {
        if (isBlank(nickname) || isBlank(email)) {
            throw new SocialRequiredInfoMissingException();
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
