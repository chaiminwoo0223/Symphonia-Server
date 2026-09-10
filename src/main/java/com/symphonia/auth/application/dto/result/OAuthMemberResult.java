package com.symphonia.auth.application.dto.result;

import com.symphonia.auth.domain.identity.SocialIdentity;
import com.symphonia.member.application.dto.command.MemberCreateCommand;
import com.symphonia.member.domain.entity.SocialProvider;

public record OAuthMemberResult(
        String socialId,
        String nickname,
        String email,
        String profileImage,
        SocialProvider socialProvider) {
    public static OAuthMemberResult from(SocialIdentity identity) {
        return new OAuthMemberResult(
                identity.socialId(),
                identity.nickname(),
                identity.email(),
                identity.profileImage(),
                SocialProvider.valueOf(identity.socialProvider()));
    }

    public MemberCreateCommand toMemberCreateCommand() {
        return new MemberCreateCommand(socialId, nickname, email, profileImage, socialProvider);
    }
}
