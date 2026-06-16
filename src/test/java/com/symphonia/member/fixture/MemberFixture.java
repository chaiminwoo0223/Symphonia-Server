package com.symphonia.member.fixture;

import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.entity.Role;
import com.symphonia.member.domain.entity.SocialProvider;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberFixture {
    GOOGLE(
            "google123",
            "구글 멤버",
            "symphonia@google.com",
            "https://image.symphonia.com/profile/google",
            SocialProvider.GOOGLE
    ),

    APPLE(
            "apple123",
            "애플 멤버",
            "symphonia@apple.com",
            "https://image.symphonia.com/profile/apple",
            SocialProvider.APPLE
    );

    private final String socialId;
    private final String nickname;
    private final String email;
    private final String profileImage;
    private final SocialProvider socialProvider;

    public Member toActive() {
        return Member.builder()
                .socialId(socialId)
                .nickname(nickname)
                .email(email)
                .profileImage(profileImage)
                .role(Role.ROLE_MEMBER)
                .socialProvider(socialProvider)
                .build();
    }

    public Member toDeleted() {
        Member member = toActive();

        member.delete();

        return member;
    }
}
