package com.symphonia.member.application.service;

import com.symphonia.global.exception.BusinessException;
import com.symphonia.member.application.dto.command.MemberCreateCommand;
import com.symphonia.member.application.dto.command.MemberUpdateCommand;
import com.symphonia.member.application.dto.result.MemberResult;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.entity.SocialProvider;
import com.symphonia.member.domain.error.MemberErrorCode;
import com.symphonia.member.domain.policy.MemberPolicy;
import com.symphonia.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberCommandService {
    private final MemberRepository memberRepository;

    public MemberResult create(MemberCreateCommand command) {
        validateDuplicated(command.socialProvider(), command.socialId());

        Member member = memberRepository.save(Member.of(command));

        return MemberResult.from(member);
    }

    public MemberResult update(Long memberId, MemberUpdateCommand command) {
        Member member = getActive(memberId);

        member.update(command);

        return MemberResult.from(member);
    }

    public void delete(Long memberId) {
        Member member = getActive(memberId);

        member.delete();
    }

    public void restore(Long memberId) {
        Member member = getDeleted(memberId);

        member.restore();
    }

    private Member getActive(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.from(MemberErrorCode.MEMBER_NOT_FOUND));

        MemberPolicy.validateNotDeleted(member);

        return member;
    }

    private Member getDeleted(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.from(MemberErrorCode.MEMBER_NOT_FOUND));

        MemberPolicy.validateDeleted(member);

        return member;
    }

    private void validateDuplicated(SocialProvider socialProvider, String socialId) {
        boolean exists = memberRepository.existsBySocialLogin(socialProvider, socialId);

        MemberPolicy.validateNotDuplicated(exists);
    }
}
