package com.symphonia.member.application.dto.result;

import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.entity.Role;
import com.symphonia.member.domain.entity.SocialProvider;

public record MemberResult(
        Long id,
        String socialId,
        String nickname,
        String email,
        String profileImage,
        Role role,
        SocialProvider socialProvider) {
    public static MemberResult from(Member member) {
        return new MemberResult(
                member.getId(),
                member.getSocialId(),
                member.getNickname(),
                member.getEmail(),
                member.getProfileImage(),
                member.getRole(),
                member.getSocialProvider());
    }
}
