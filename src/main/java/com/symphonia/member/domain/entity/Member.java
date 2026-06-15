package com.symphonia.member.domain.entity;

import com.symphonia.global.common.entity.BaseTimeEntity;
import com.symphonia.member.application.dto.command.MemberCreateCommand;
import com.symphonia.member.application.dto.command.MemberUpdateCommand;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

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

    public static Member of(MemberCreateCommand command) {
        return Member.builder()
                .socialId(command.socialId())
                .nickname(command.nickname())
                .email(command.email())
                .profileImage(command.profileImage())
                .role(Role.ROLE_MEMBER)
                .socialProvider(command.socialProvider())
                .build();
    }

    public Member update(MemberUpdateCommand command) {
        this.nickname = command.nickname();
        this.profileImage = command.profileImage();
        return this;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deletedAt = null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
