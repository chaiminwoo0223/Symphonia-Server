package com.symphonia.auth.application.service;

import com.symphonia.auth.application.dto.result.TokenResult;
import com.symphonia.auth.application.usecase.IssueTokenUseCase;
import com.symphonia.auth.application.usecase.RefreshUseCase;
import com.symphonia.auth.domain.exception.RefreshTokenNotFoundException;
import com.symphonia.auth.domain.repository.RefreshTokenRepository;
import com.symphonia.common.annotation.CommandService;
import com.symphonia.member.application.dto.result.MemberResult;
import com.symphonia.member.application.usecase.GetMemberUseCase;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@CommandService
@RequiredArgsConstructor
public class RefreshService implements RefreshUseCase {
    private final RefreshTokenRepository refreshTokenRepository;
    private final IssueTokenUseCase issueTokenUseCase;
    private final GetMemberUseCase getMemberUseCase;

    @Override
    public TokenResult refresh(String refreshToken) {
        String memberId =
                Optional.ofNullable(refreshToken)
                        .flatMap(refreshTokenRepository::consume)
                        .orElseThrow(RefreshTokenNotFoundException::new);
        MemberResult member = getMemberUseCase.getById(Long.parseLong(memberId));

        refreshTokenRepository.delete(memberId);

        return issueTokenUseCase.issue(memberId, member.role().name());
    }
}
