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
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberCommandService {
    private final MemberRepository memberRepository;

    public MemberResult create(MemberCreateCommand command) {
        boolean exists = memberRepository.existsBySocialLogin(command.socialProvider(), command.socialId());
        MemberPolicy.validateNotDuplicated(exists);

        Member member = Member.of(command.socialId(), command.nickname(), command.email(), command.profileImage(), command.socialProvider());
        Member savedMember = memberRepository.save(member);

        return MemberResult.from(savedMember);
    }

    public MemberResult update(Long memberId, MemberUpdateCommand command) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.from(MemberErrorCode.MEMBER_NOT_FOUND));

        member.update(command.nickname());

        return MemberResult.from(member);
    }

    public void delete(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.from(MemberErrorCode.MEMBER_NOT_FOUND));

        member.withdraw();
    }
}
