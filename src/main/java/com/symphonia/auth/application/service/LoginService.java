package com.symphonia.auth.application.service;

import com.symphonia.auth.application.dto.command.LoginCommand;
import com.symphonia.auth.application.dto.result.OAuthMemberResult;
import com.symphonia.auth.application.dto.result.TokenResult;
import com.symphonia.auth.application.usecase.IssueTokenUseCase;
import com.symphonia.auth.application.usecase.LoginUseCase;
import com.symphonia.auth.application.usecase.VerifySocialTokenUseCase;
import com.symphonia.common.audit.AuditAction;
import com.symphonia.common.audit.Audited;
import com.symphonia.member.application.dto.result.MemberResult;
import com.symphonia.member.application.usecase.GetMemberUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService implements LoginUseCase {
    private final VerifySocialTokenUseCase verifySocialTokenUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final IssueTokenUseCase issueTokenUseCase;

    @Override
    @Audited(action = AuditAction.LOGIN)
    public TokenResult login(LoginCommand command) {
        OAuthMemberResult identity =
                verifySocialTokenUseCase.verify(command.provider(), command.token());
        MemberResult member =
                getMemberUseCase.getBySocialLogin(identity.socialProvider(), identity.socialId());

        return issueTokenUseCase.issue(String.valueOf(member.id()), member.role().name());
    }
}
