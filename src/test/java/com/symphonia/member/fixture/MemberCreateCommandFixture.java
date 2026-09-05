package com.symphonia.member.fixture;

import com.symphonia.member.application.dto.command.MemberCreateCommand;
import com.symphonia.member.domain.entity.SocialProvider;

public class MemberCreateCommandFixture {
  private String socialId;
  private String nickname;
  private String email;
  private String profileImage;
  private SocialProvider socialProvider;

  public MemberCreateCommandFixture(MemberFixture fixture) {
    this.socialId = fixture.getSocialId();
    this.nickname = fixture.getNickname();
    this.email = fixture.getEmail();
    this.profileImage = fixture.getProfileImage();
    this.socialProvider = fixture.getSocialProvider();
  }

  public MemberCreateCommandFixture socialId(String socialId) {
    this.socialId = socialId;
    return this;
  }

  public MemberCreateCommandFixture nickname(String nickname) {
    this.nickname = nickname;
    return this;
  }

  public MemberCreateCommandFixture email(String email) {
    this.email = email;
    return this;
  }

  public MemberCreateCommandFixture profileImage(String profileImage) {
    this.profileImage = profileImage;
    return this;
  }

  public MemberCreateCommandFixture socialProvider(SocialProvider socialProvider) {
    this.socialProvider = socialProvider;
    return this;
  }

  public MemberCreateCommand build() {
    return new MemberCreateCommand(socialId, nickname, email, profileImage, socialProvider);
  }
}
