package com.symphonia.member.infrastructure.jpa;

import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.entity.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberJpaRepository extends JpaRepository<Member, Long> {
    Optional<Member> findBySocialProviderAndSocialId(SocialProvider socialProvider, String socialId);

    boolean existsBySocialProviderAndSocialId(SocialProvider socialProvider, String socialId);
}
