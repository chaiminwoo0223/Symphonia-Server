package com.symphonia.member.infrastructure.jpa;

import com.symphonia.global.common.entity.BaseTimeEntity;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.entity.Role;
import com.symphonia.member.domain.entity.SocialProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String socialId;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String email;

    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialProvider socialProvider;

    public static MemberJpaEntity from(Member member) {
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

    public Member toDomain() {
        return Member.builder()
                .id(id)
                .socialId(socialId)
                .nickname(nickname)
                .email(email)
                .profileImage(profileImage)
                .role(role)
                .socialProvider(socialProvider)
                .build();
    }
}
