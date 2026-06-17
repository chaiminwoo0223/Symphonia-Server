package com.symphonia.member.fixture;

import com.symphonia.member.application.dto.command.MemberRestoreCommand;
import com.symphonia.member.domain.entity.SocialProvider;

public class MemberRestoreCommandFixture {
    private SocialProvider socialProvider;
    private String socialId;

    public MemberRestoreCommandFixture(MemberFixture fixture) {
        this.socialProvider = fixture.getSocialProvider();
        this.socialId = fixture.getSocialId();
    }

    public MemberRestoreCommandFixture socialProvider(SocialProvider socialProvider) {
        this.socialProvider = socialProvider;
        return this;
    }

    public MemberRestoreCommandFixture socialId(String socialId) {
        this.socialId = socialId;
        return this;
    }

    public MemberRestoreCommand build() {
        return new MemberRestoreCommand(socialProvider, socialId);
    }
}
