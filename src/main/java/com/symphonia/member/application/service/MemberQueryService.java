package com.symphonia.member.application.service;

import com.symphonia.common.annotation.QueryService;
import com.symphonia.member.application.dto.result.MemberResult;
import com.symphonia.member.application.usecase.GetMemberUseCase;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.entity.SocialProvider;
import com.symphonia.member.domain.exception.MemberNotFoundException;
import com.symphonia.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;

@QueryService
@RequiredArgsConstructor
public class MemberQueryService implements GetMemberUseCase {
    private final MemberRepository memberRepository;

    @Override
    public MemberResult getBySocialLogin(SocialProvider socialProvider, String socialId) {
        Member member =
                memberRepository
                        .findBySocialLogin(socialProvider, socialId)
                        .orElseThrow(MemberNotFoundException::new);

        return MemberResult.from(member);
    }

    @Override
    public MemberResult getById(Long memberId) {
        Member member =
                memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

        return MemberResult.from(member);
    }
}
