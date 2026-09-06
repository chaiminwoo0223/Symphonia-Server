package com.symphonia.member.domain.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Member {

    private Long id;
    private String socialId;
    private String nickname;
    private String email;
    private String profileImage;
    private Role role;
    private SocialProvider socialProvider;

    public static Member of(
            String socialId,
            String nickname,
            String email,
            String profileImage,
            SocialProvider socialProvider) {
        return Member.builder()
                .socialId(socialId)
                .nickname(nickname)
                .email(email)
                .profileImage(profileImage)
                .role(Role.ROLE_MEMBER)
                .socialProvider(socialProvider)
                .build();
    }

    public void update(String nickname) {
        this.nickname = nickname;
    }
}
