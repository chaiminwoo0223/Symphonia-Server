package com.symphonia.member.infrastructure.jpa;

import com.symphonia.member.domain.entity.Member;

public final class MemberMapper {
    private MemberMapper() {}

    public static Member toDomain(MemberJpaEntity entity) {
        return Member.builder()
                .id(entity.getId())
                .socialId(entity.getSocialId())
                .nickname(entity.getNickname())
                .email(entity.getEmail())
                .profileImage(entity.getProfileImage())
                .role(entity.getRole())
                .socialProvider(entity.getSocialProvider())
                .build();
    }

    public static MemberJpaEntity toEntity(Member member) {
        return MemberJpaEntity.builder()
                .id(member.getId())
                .socialId(member.getSocialId())
                .nickname(member.getNickname())
                .email(member.getEmail())
                .profileImage(member.getProfileImage())
                .role(member.getRole())
                .socialProvider(member.getSocialProvider())
                .build();
    }
}
