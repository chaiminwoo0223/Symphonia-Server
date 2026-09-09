package com.symphonia.auth.application.service;

import com.symphonia.auth.application.dto.command.SignupCommand;
import com.symphonia.auth.application.dto.result.OAuthMemberResult;
import com.symphonia.auth.application.dto.result.TokenResult;
import com.symphonia.auth.application.usecase.ExchangeSocialCodeUseCase;
import com.symphonia.auth.application.usecase.IssueTokenUseCase;
import com.symphonia.auth.application.usecase.SignupUseCase;
import com.symphonia.common.annotation.CommandService;
import com.symphonia.member.application.dto.command.MemberCreateCommand;
import com.symphonia.member.application.dto.result.MemberResult;
import com.symphonia.member.application.usecase.CreateMemberUseCase;
import lombok.RequiredArgsConstructor;

@CommandService
@RequiredArgsConstructor
public class SignupService implements SignupUseCase {
    private final ExchangeSocialCodeUseCase exchangeSocialCodeUseCase;
    private final CreateMemberUseCase createMemberUseCase;
    private final IssueTokenUseCase issueTokenUseCase;

    @Override
    public TokenResult signup(SignupCommand command) {
        OAuthMemberResult identity =
                exchangeSocialCodeUseCase.exchange(command.provider(), command.code());
        MemberResult member =
                createMemberUseCase.create(
                        new MemberCreateCommand(
                                identity.socialId(),
                                identity.nickname(),
                                identity.email(),
                                identity.profileImage(),
                                identity.socialProvider()));

        return issueTokenUseCase.issue(String.valueOf(member.id()), member.role().name());
    }
}
