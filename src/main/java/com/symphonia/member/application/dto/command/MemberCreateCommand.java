package com.symphonia.member.application.dto.command;

import com.symphonia.member.domain.entity.SocialProvider;

public record MemberCreateCommand(
        String socialId,
        String nickname,
        String email,
        String profileImage,
        SocialProvider socialProvider) {}
