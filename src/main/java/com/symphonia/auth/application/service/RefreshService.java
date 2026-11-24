package com.symphonia.auth.application.service;

import com.symphonia.auth.application.dto.command.RefreshCommand;
import com.symphonia.auth.application.dto.result.TokenResult;
import com.symphonia.auth.application.usecase.IssueTokenUseCase;
import com.symphonia.auth.application.usecase.RefreshUseCase;
import com.symphonia.auth.domain.exception.RefreshTokenNotFoundException;
import com.symphonia.auth.domain.repository.RefreshTokenRepository;
import com.symphonia.common.annotation.CommandService;
import com.symphonia.common.audit.AuditAction;
import com.symphonia.common.audit.Audited;
import com.symphonia.member.application.dto.result.MemberResult;
import com.symphonia.member.application.usecase.GetMemberUseCase;
import lombok.RequiredArgsConstructor;

@CommandService
@RequiredArgsConstructor
public class RefreshService implements RefreshUseCase {
    private final RefreshTokenRepository refreshTokenRepository;
    private final IssueTokenUseCase issueTokenUseCase;
    private final GetMemberUseCase getMemberUseCase;

    // 새 토큰을 발급한 뒤에 기존 토큰을 소비한다. 발급이 실패하면 기존 토큰이 그대로 남아 같은 토큰으로 다시 시도할 수 있다.
    // 같은 토큰으로 동시에 들어온 요청은 소비에 성공한 하나만 새 토큰을 돌려받는다. 나머지가 발급한 토큰은 아무에게도 전달되지 않고 만료된다.
    @Override
    @Audited(action = AuditAction.TOKEN_REFRESH)
    public TokenResult refresh(RefreshCommand command) {
        String memberId =
                refreshTokenRepository
                        .findMemberIdByValue(command.refreshToken())
                        .orElseThrow(RefreshTokenNotFoundException::new);
        MemberResult member = getMemberUseCase.getById(Long.parseLong(memberId));
        TokenResult tokenResult = issueTokenUseCase.issue(memberId, member.role().name());

        refreshTokenRepository
                .consume(command.refreshToken())
                .orElseThrow(RefreshTokenNotFoundException::new);

        return tokenResult;
    }
}
