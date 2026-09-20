package com.symphonia.member.infrastructure.jpa;

import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.entity.SocialProvider;
import com.symphonia.member.domain.policy.MemberPolicy;
import com.symphonia.member.domain.repository.MemberRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepository {
    private final MemberJpaRepository memberJpaRepository;

    @Override
    public Optional<Member> findBySocialLogin(SocialProvider socialProvider, String socialId) {
        return memberJpaRepository
                .findBySocialProviderAndSocialId(socialProvider, socialId)
                .map(MemberJpaEntity::toDomain);
    }

    @Override
    public Optional<Member> findById(Long memberId) {
        return memberJpaRepository.findById(memberId).map(MemberJpaEntity::toDomain);
    }

    @Override
    public Member save(Member member) {
        try {
            MemberJpaEntity savedEntity = memberJpaRepository.save(MemberJpaEntity.from(member));

            return savedEntity.toDomain();
        } catch (DataIntegrityViolationException e) {
            MemberPolicy.validateNotDuplicated(isSocialLoginViolation(e));
            throw e;
        }
    }

    @Override
    public boolean existsBySocialLogin(SocialProvider socialProvider, String socialId) {
        return memberJpaRepository.existsBySocialProviderAndSocialId(socialProvider, socialId);
    }

    @Override
    public void delete(Member member) {
        memberJpaRepository.deleteById(member.getId());
    }

    private boolean isSocialLoginViolation(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException violation
                && "uk_member_social_login".equalsIgnoreCase(violation.getConstraintName());
    }
}
