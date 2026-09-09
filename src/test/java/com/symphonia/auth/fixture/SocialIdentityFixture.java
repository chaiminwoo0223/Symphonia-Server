package com.symphonia.auth.fixture;

import com.symphonia.auth.domain.identity.SocialIdentity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SocialIdentityFixture {
    KAKAO(
            "kakao123",
            "카카오 멤버",
            "symphonia@kakao.com",
            "https://image.symphonia.com/profile/kakao",
            "KAKAO"),

    GOOGLE(
            "google123",
            "구글 멤버",
            "symphonia@google.com",
            "https://image.symphonia.com/profile/google",
            "GOOGLE");

    private final String socialId;
    private final String nickname;
    private final String email;
    private final String profileImage;
    private final String socialProvider;

    public SocialIdentity create() {
        return new SocialIdentity(socialId, nickname, email, profileImage, socialProvider);
    }
}
