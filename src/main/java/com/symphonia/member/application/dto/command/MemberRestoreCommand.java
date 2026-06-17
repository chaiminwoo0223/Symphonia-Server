package com.symphonia.member.application.dto.command;

import com.symphonia.member.domain.entity.SocialProvider;

public record MemberRestoreCommand(
        String socialId,
        SocialProvider socialProvider
) {
}
