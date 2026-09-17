package com.symphonia.member.fixture;

import com.symphonia.member.application.dto.command.CreateMemberCommand;
import com.symphonia.member.domain.entity.SocialProvider;

public class CreateMemberCommandFixture {
    private String socialId;
    private String nickname;
    private String email;
    private String profileImage;
    private SocialProvider socialProvider;

    public CreateMemberCommandFixture(MemberFixture fixture) {
        this.socialId = fixture.getSocialId();
        this.nickname = fixture.getNickname();
        this.email = fixture.getEmail();
        this.profileImage = fixture.getProfileImage();
        this.socialProvider = fixture.getSocialProvider();
    }

    public CreateMemberCommandFixture socialId(String socialId) {
        this.socialId = socialId;
        return this;
    }

    public CreateMemberCommandFixture nickname(String nickname) {
        this.nickname = nickname;
        return this;
    }

    public CreateMemberCommandFixture email(String email) {
        this.email = email;
        return this;
    }

    public CreateMemberCommandFixture profileImage(String profileImage) {
        this.profileImage = profileImage;
        return this;
    }

    public CreateMemberCommandFixture socialProvider(SocialProvider socialProvider) {
        this.socialProvider = socialProvider;
        return this;
    }

    public CreateMemberCommand build() {
        return new CreateMemberCommand(socialId, nickname, email, profileImage, socialProvider);
    }
}
