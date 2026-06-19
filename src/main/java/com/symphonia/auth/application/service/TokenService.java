package com.symphonia.auth.application.service;

import com.symphonia.auth.application.dto.result.TokenResult;
import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.auth.domain.repository.BlacklistAccessTokenRepository;
import com.symphonia.auth.domain.repository.RefreshTokenRepository;
import com.symphonia.auth.infrastructure.provider.AccessTokenProvider;
import com.symphonia.auth.infrastructure.provider.RefreshTokenProvider;
import com.symphonia.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenProvider refreshTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BlacklistAccessTokenRepository blacklistAccessTokenRepository;

    public TokenResult issue(String memberId, String role) {
        String accessToken = accessTokenProvider.generate(memberId, role);
        String refreshToken = refreshTokenProvider.generate();
        long refreshTokenExpirationTime = refreshTokenProvider.getExpirationTime();

        refreshTokenRepository.save(refreshToken, memberId, refreshTokenExpirationTime);

        return TokenResult.of(accessToken, refreshToken);
    }

    public TokenResult reissue(String refreshToken, String role) {
        String memberId = getMemberIdByRefreshToken(refreshToken);

        refreshTokenRepository.delete(memberId);

        return issue(memberId, role);
    }

    public void revoke(String accessToken) {
        String memberId = accessTokenProvider.getMemberId(accessToken);
        long accessTokenRemainingTime = accessTokenProvider.getRemainingTime(accessToken);

        blacklistAccessTokenRepository.save(accessToken, memberId, accessTokenRemainingTime);
        refreshTokenRepository.delete(memberId);
    }

    // 관리자 전용
    public void forceRevoke(String memberId) {
        refreshTokenRepository.delete(memberId);
    }

    private String getMemberIdByRefreshToken(String refreshToken) {
        return refreshTokenRepository.findMemberIdByValue(refreshToken)
                .orElseThrow(() -> BusinessException.from(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));
    }
}
