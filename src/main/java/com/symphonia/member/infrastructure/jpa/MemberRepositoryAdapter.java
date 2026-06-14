package com.symphonia.member.infrastructure.jpa;

import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.entity.SocialProvider;
import com.symphonia.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryAdapter implements MemberRepository {
    private final MemberJpaRepository memberJpaRepository;

    @Override
    public Optional<Member> findBySocialLogin(SocialProvider socialProvider, String socialId) {
        return memberJpaRepository.findBySocialProviderAndSocialId(socialProvider, socialId);
    }

    @Override
    public Optional<Member> findById(Long id) {
        return memberJpaRepository.findById(id);
    }

    @Override
    public Member save(Member member) {
        return memberJpaRepository.save(member);
    }

    @Override
    public boolean existsBySocialLogin(SocialProvider socialProvider, String socialId) {
        return memberJpaRepository.existsBySocialProviderAndSocialId(socialProvider, socialId);
    }
}
