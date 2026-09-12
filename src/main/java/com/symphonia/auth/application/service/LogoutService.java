package com.symphonia.auth.application.service;

import com.symphonia.auth.application.dto.command.LogoutCommand;
import com.symphonia.auth.application.usecase.LogoutUseCase;
import com.symphonia.auth.domain.repository.BlacklistAccessTokenRepository;
import com.symphonia.auth.domain.repository.RefreshTokenRepository;
import com.symphonia.auth.infrastructure.provider.AccessTokenProvider;
import com.symphonia.common.annotation.CommandService;
import com.symphonia.common.audit.AuditEvent;
import com.symphonia.common.audit.Audited;
import lombok.RequiredArgsConstructor;

@CommandService
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {
    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BlacklistAccessTokenRepository blacklistAccessTokenRepository;

    @Override
    @Audited(event = AuditEvent.LOGOUT)
    public void logout(LogoutCommand command) {
        String accessToken = command.accessToken();
        String memberId = accessTokenProvider.getMemberId(accessToken);
        long accessTokenRemainingTime = accessTokenProvider.getRemainingTime(accessToken);

        blacklistAccessTokenRepository.save(accessToken, memberId, accessTokenRemainingTime);
        refreshTokenRepository.delete(memberId);
    }
}
