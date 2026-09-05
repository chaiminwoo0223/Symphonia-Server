package com.symphonia.member.fixture;

import com.symphonia.member.application.dto.command.MemberUpdateCommand;

public class MemberUpdateCommandFixture {
  private String nickname;

  public MemberUpdateCommandFixture nickname(String nickname) {
    this.nickname = nickname;
    return this;
  }

  public MemberUpdateCommand build() {
    return new MemberUpdateCommand(nickname);
  }
}
