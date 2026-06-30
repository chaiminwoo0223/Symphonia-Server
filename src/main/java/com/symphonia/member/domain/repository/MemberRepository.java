package com.symphonia.member.domain.repository;

import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.entity.SocialProvider;

import java.util.Optional;

public interface MemberRepository {
    Optional<Member> findBySocialLogin(SocialProvider socialProvider, String socialId);

    Optional<Member> findById(Long memberId);

    Member save(Member member);

    boolean existsBySocialLogin(SocialProvider socialProvider, String socialId);

    void delete(Member member);
}
