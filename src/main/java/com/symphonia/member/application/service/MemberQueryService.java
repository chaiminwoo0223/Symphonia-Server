package com.symphonia.member.application.service;

import com.symphonia.global.common.annotation.QueryService;
import com.symphonia.global.exception.BusinessException;
import com.symphonia.member.application.dto.result.MemberResult;
import com.symphonia.member.application.usecase.GetMemberUseCase;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.entity.SocialProvider;
import com.symphonia.member.domain.error.MemberErrorCode;
import com.symphonia.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;

@QueryService
@RequiredArgsConstructor
public class MemberQueryService implements GetMemberUseCase {
    private final MemberRepository memberRepository;

    public MemberResult getBySocialLogin(SocialProvider socialProvider, String socialId) {
        Member member =
                memberRepository
                        .findBySocialLogin(socialProvider, socialId)
                        .orElseThrow(
                                () -> BusinessException.from(MemberErrorCode.MEMBER_NOT_FOUND));

        return MemberResult.from(member);
    }

    @Override
    public MemberResult getById(Long memberId) {
        Member member =
                memberRepository
                        .findById(memberId)
                        .orElseThrow(
                                () -> BusinessException.from(MemberErrorCode.MEMBER_NOT_FOUND));

        return MemberResult.from(member);
    }
}
