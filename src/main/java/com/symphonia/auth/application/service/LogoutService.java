package com.symphonia.auth.application.service;

import com.symphonia.auth.application.dto.command.LogoutCommand;
import com.symphonia.auth.application.dto.result.LogoutResult;
import com.symphonia.auth.application.usecase.LogoutUseCase;
import com.symphonia.auth.domain.provider.AccessTokenProvider;
import com.symphonia.auth.domain.repository.BlacklistAccessTokenRepository;
import com.symphonia.auth.domain.repository.RefreshTokenRepository;
import com.symphonia.common.annotation.CommandService;
import com.symphonia.common.audit.AuditAction;
import com.symphonia.common.audit.Audited;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

// 로그아웃은 이 기기의 세션만 끊고, 토큰이 없거나 만료돼도 실패하지 않는다.
// 리프레시 토큰은 쿠키로 받은 것 하나만 지우므로 같은 회원의 다른 기기 토큰은 유지된다.
// 액세스 토큰은 아직 유효할 때만 블랙리스트에 올린다. 만료된 토큰은 이미 쓸 수 없기 때문이다.
@CommandService
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {
    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BlacklistAccessTokenRepository blacklistAccessTokenRepository;

    @Override
    @Audited(action = AuditAction.LOGOUT)
    public LogoutResult logout(LogoutCommand command) {
        String memberId =
                Optional.ofNullable(command.refreshToken())
                        .flatMap(refreshTokenRepository::consume)
                        .orElse(null);
        Optional.ofNullable(command.accessToken())
                .filter(accessTokenProvider::validate)
                .ifPresent(this::blacklist);

        return LogoutResult.of(memberId);
    }

    private void blacklist(String accessToken) {
        blacklistAccessTokenRepository.save(
                accessToken,
                accessTokenProvider.getMemberId(accessToken),
                accessTokenProvider.getRemainingTime(accessToken));
    }
}
