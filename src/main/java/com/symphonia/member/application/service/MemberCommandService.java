package com.symphonia.member.application.service;

import com.symphonia.global.exception.BusinessException;
import com.symphonia.member.application.dto.command.MemberCreateCommand;
import com.symphonia.member.application.dto.command.MemberRestoreCommand;
import com.symphonia.member.application.dto.command.MemberUpdateCommand;
import com.symphonia.member.application.dto.result.MemberResult;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.entity.SocialProvider;
import com.symphonia.member.domain.error.MemberErrorCode;
import com.symphonia.member.domain.policy.MemberPolicy;
import com.symphonia.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberCommandService {
    private final MemberRepository memberRepository;

    public MemberResult create(MemberCreateCommand command) {
        validateDuplicated(command.socialProvider(), command.socialId());

        Member member = memberRepository.save(Member.of(command));

        return MemberResult.from(member);
    }

    public MemberResult update(Long memberId, MemberUpdateCommand command) {
        Member member = getActiveById(memberId);

        member.update(command);

        return MemberResult.from(member);
    }

    public void delete(Long memberId) {
        Member member = getActiveById(memberId);

        member.withdraw();
    }

    public MemberResult restore(MemberRestoreCommand command) {
        Member member = getDeletedBySocialLogin(command.socialProvider(), command.socialId());

        member.restore();

        return MemberResult.from(member);
    }

    private Member getActiveById(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.from(MemberErrorCode.MEMBER_NOT_FOUND));

        MemberPolicy.validateNotWithdrawn(member);

        return member;
    }

    private Member getDeletedBySocialLogin(SocialProvider socialProvider, String socialId) {
        Member member = memberRepository.findBySocialLogin(socialProvider, socialId)
                .orElseThrow(() -> BusinessException.from(MemberErrorCode.MEMBER_NOT_FOUND));

        MemberPolicy.validateWithdrawn(member);

        return member;
    }

    private void validateDuplicated(SocialProvider socialProvider, String socialId) {
        boolean exists = memberRepository.existsBySocialLogin(socialProvider, socialId);

        MemberPolicy.validateNotDuplicated(exists);
    }
}
