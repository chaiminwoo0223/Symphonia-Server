package com.symphonia.auth.application.service;

import com.symphonia.auth.application.dto.result.TokenResult;
import com.symphonia.auth.application.usecase.IssueTokenUseCase;
import com.symphonia.auth.domain.repository.RefreshTokenRepository;
import com.symphonia.auth.infrastructure.provider.AccessTokenProvider;
import com.symphonia.auth.infrastructure.provider.RefreshTokenProvider;
import com.symphonia.common.annotation.CommandService;
import lombok.RequiredArgsConstructor;

@CommandService
@RequiredArgsConstructor
public class IssueTokenService implements IssueTokenUseCase {
    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenProvider refreshTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public TokenResult issue(String memberId, String role) {
        String accessToken = accessTokenProvider.generate(memberId, role);
        String refreshToken = refreshTokenProvider.generate();
        long refreshTokenExpirationTime = refreshTokenProvider.getExpirationTime();

        refreshTokenRepository.save(refreshToken, memberId, refreshTokenExpirationTime);

        return TokenResult.of(accessToken, refreshToken);
    }
}
