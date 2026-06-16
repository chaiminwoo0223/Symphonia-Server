package com.symphonia.member.fixture;

import com.symphonia.member.application.dto.command.MemberUpdateCommand;

public class MemberUpdateCommandFixture {
    private String nickname;
    private String profileImage;

    public MemberUpdateCommandFixture nickname(String nickname) {
        this.nickname = nickname;
        return this;
    }

    public MemberUpdateCommandFixture profileImage(String profileImage) {
        this.profileImage = profileImage;
        return this;
    }

    public MemberUpdateCommand build() {
        return new MemberUpdateCommand(nickname, profileImage);
    }
}
