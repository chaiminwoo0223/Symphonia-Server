package com.symphonia.member.application.service;

import com.symphonia.auth.application.usecase.LogoutUseCase;
import com.symphonia.common.annotation.CommandService;
import com.symphonia.member.application.dto.command.MemberCreateCommand;
import com.symphonia.member.application.dto.command.MemberUpdateCommand;
import com.symphonia.member.application.dto.result.MemberResult;
import com.symphonia.member.application.usecase.CreateMemberUseCase;
import com.symphonia.member.application.usecase.DeleteMemberUseCase;
import com.symphonia.member.application.usecase.UpdateMemberUseCase;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.domain.exception.MemberNotFoundException;
import com.symphonia.member.domain.policy.MemberPolicy;
import com.symphonia.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;

@CommandService
@RequiredArgsConstructor
public class MemberCommandService
        implements CreateMemberUseCase, UpdateMemberUseCase, DeleteMemberUseCase {
    private final MemberRepository memberRepository;
    private final LogoutUseCase logoutUseCase;

    @Override
    public MemberResult create(MemberCreateCommand command) {
        boolean exists =
                memberRepository.existsBySocialLogin(command.socialProvider(), command.socialId());
        MemberPolicy.validateNotDuplicated(exists);

        Member member =
                Member.of(
                        command.socialId(),
                        command.nickname(),
                        command.email(),
                        command.profileImage(),
                        command.socialProvider());
        Member savedMember = memberRepository.save(member);

        return MemberResult.from(savedMember);
    }

    @Override
    public MemberResult update(Long memberId, MemberUpdateCommand command) {
        Member member =
                memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

        member.update(command.nickname());
        Member updatedMember = memberRepository.save(member);

        return MemberResult.from(updatedMember);
    }

    @Override
    public void delete(Long memberId, String accessToken) {
        Member member =
                memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

        memberRepository.delete(member);
        logoutUseCase.logout(accessToken);
    }
}
