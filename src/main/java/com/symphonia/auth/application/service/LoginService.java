package com.symphonia.auth.application.service;

import com.symphonia.auth.application.dto.command.LoginCommand;
import com.symphonia.auth.application.dto.result.OAuthMemberResult;
import com.symphonia.auth.application.dto.result.TokenResult;
import com.symphonia.auth.application.usecase.ExchangeSocialCodeUseCase;
import com.symphonia.auth.application.usecase.IssueTokenUseCase;
import com.symphonia.auth.application.usecase.LoginUseCase;
import com.symphonia.common.annotation.CommandService;
import com.symphonia.member.application.dto.result.MemberResult;
import com.symphonia.member.application.usecase.GetMemberUseCase;
import lombok.RequiredArgsConstructor;

@CommandService
@RequiredArgsConstructor
public class LoginService implements LoginUseCase {
    private final ExchangeSocialCodeUseCase exchangeSocialCodeUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final IssueTokenUseCase issueTokenUseCase;

    @Override
    public TokenResult login(LoginCommand command) {
        OAuthMemberResult identity =
                exchangeSocialCodeUseCase.exchange(command.provider(), command.code());
        MemberResult member =
                getMemberUseCase.getBySocialLogin(identity.socialProvider(), identity.socialId());

        return issueTokenUseCase.issue(String.valueOf(member.id()), member.role().name());
    }
}
