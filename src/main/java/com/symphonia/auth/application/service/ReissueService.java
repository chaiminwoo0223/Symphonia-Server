package com.symphonia.auth.application.service;

import com.symphonia.auth.application.dto.result.TokenResult;
import com.symphonia.auth.application.usecase.IssueTokenUseCase;
import com.symphonia.auth.application.usecase.ReissueUseCase;
import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.auth.domain.repository.RefreshTokenRepository;
import com.symphonia.common.annotation.CommandService;
import com.symphonia.common.exception.BusinessException;
import com.symphonia.member.application.dto.result.MemberResult;
import com.symphonia.member.application.usecase.GetMemberUseCase;
import lombok.RequiredArgsConstructor;

@CommandService
@RequiredArgsConstructor
public class ReissueService implements ReissueUseCase {
    private final RefreshTokenRepository refreshTokenRepository;
    private final IssueTokenUseCase issueTokenUseCase;
    private final GetMemberUseCase getMemberUseCase;

    @Override
    public TokenResult reissue(String refreshToken) {
        String memberId =
                refreshTokenRepository
                        .findMemberIdByValue(refreshToken)
                        .orElseThrow(
                                () ->
                                        BusinessException.from(
                                                AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));
        MemberResult member = getMemberUseCase.getById(Long.parseLong(memberId));

        refreshTokenRepository.delete(memberId);

        return issueTokenUseCase.issue(memberId, member.role().name());
    }
}
