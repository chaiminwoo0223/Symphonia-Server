package com.symphonia.member.fixture;

import com.symphonia.member.application.dto.result.MemberResult;
import com.symphonia.member.domain.entity.Role;
import com.symphonia.member.domain.entity.SocialProvider;

public class MemberResultFixture {
    private final String socialId;
    private final String nickname;
    private final String email;
    private final String profileImage;
    private final Role role;
    private final SocialProvider socialProvider;
    private Long id;

    public MemberResultFixture(MemberFixture fixture) {
        this.socialId = fixture.getSocialId();
        this.nickname = fixture.getNickname();
        this.email = fixture.getEmail();
        this.profileImage = fixture.getProfileImage();
        this.role = Role.ROLE_MEMBER;
        this.socialProvider = fixture.getSocialProvider();
    }

    public MemberResultFixture id(Long id) {
        this.id = id;
        return this;
    }

    public MemberResult build() {
        return new MemberResult(id, socialId, nickname, email, profileImage, role, socialProvider);
    }
}
