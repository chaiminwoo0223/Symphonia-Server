package com.symphonia.member.infrastructure.jpa;

import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.entity.SocialProvider;
import com.symphonia.member.domain.repository.MemberRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepository {
  private final MemberJpaRepository memberJpaRepository;

  @Override
  public Optional<Member> findBySocialLogin(SocialProvider socialProvider, String socialId) {
    return memberJpaRepository.findBySocialProviderAndSocialId(socialProvider, socialId);
  }

  @Override
  public Optional<Member> findById(Long memberId) {
    return memberJpaRepository.findById(memberId);
  }

  @Override
  public Member save(Member member) {
    return memberJpaRepository.save(member);
  }

  @Override
  public boolean existsBySocialLogin(SocialProvider socialProvider, String socialId) {
    return memberJpaRepository.existsBySocialProviderAndSocialId(socialProvider, socialId);
  }

  @Override
  public void delete(Member member) {
    memberJpaRepository.delete(member);
  }
}
