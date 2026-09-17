package com.symphonia.member.fixture;

import com.symphonia.member.application.dto.command.UpdateMemberCommand;

public class UpdateMemberCommandFixture {
    private String nickname;

    public UpdateMemberCommandFixture nickname(String nickname) {
        this.nickname = nickname;
        return this;
    }

    public UpdateMemberCommand build() {
        return new UpdateMemberCommand(nickname);
    }
}
