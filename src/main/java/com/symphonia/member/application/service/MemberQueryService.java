package com.symphonia.member.application.service;

import com.symphonia.global.exception.BusinessException;
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
public class MemberQueryService {
    private final MemberRepository memberRepository;

    public MemberResult getActiveBySocialLogin(SocialProvider socialProvider, String socialId) {
        Member member = memberRepository.findBySocialLogin(socialProvider, socialId)
                .orElseThrow(() -> BusinessException.from(MemberErrorCode.MEMBER_NOT_FOUND));

        MemberPolicy.validateNotDeleted(member);

        return MemberResult.from(member);
    }

    public MemberResult getActiveById(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.from(MemberErrorCode.MEMBER_NOT_FOUND));

        MemberPolicy.validateNotDeleted(member);

        return MemberResult.from(member);
    }
}
