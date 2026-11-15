package com.symphonia.auth.application.service;

import com.symphonia.auth.application.dto.command.SignupCommand;
import com.symphonia.auth.application.dto.result.OAuthMemberResult;
import com.symphonia.auth.application.dto.result.TokenResult;
import com.symphonia.auth.application.usecase.IssueTokenUseCase;
import com.symphonia.auth.application.usecase.SignupUseCase;
import com.symphonia.auth.application.usecase.VerifySocialTokenUseCase;
import com.symphonia.common.audit.AuditAction;
import com.symphonia.common.audit.Audited;
import com.symphonia.member.application.dto.result.MemberResult;
import com.symphonia.member.application.usecase.CreateMemberUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignupService implements SignupUseCase {
    private final VerifySocialTokenUseCase verifySocialTokenUseCase;
    private final CreateMemberUseCase createMemberUseCase;
    private final IssueTokenUseCase issueTokenUseCase;

    @Override
    @Audited(action = AuditAction.SIGNUP)
    public TokenResult signup(SignupCommand command) {
        OAuthMemberResult result =
                verifySocialTokenUseCase.verify(command.provider(), command.token());
        MemberResult member = createMemberUseCase.create(result.toCreateMemberCommand());

        return issueTokenUseCase.issue(String.valueOf(member.id()), member.role().name());
    }
}
