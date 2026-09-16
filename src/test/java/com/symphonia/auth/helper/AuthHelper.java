package com.symphonia.auth.helper;

import com.symphonia.auth.domain.repository.RefreshTokenRepository;
import com.symphonia.auth.infrastructure.provider.AccessTokenProvider;
import com.symphonia.auth.infrastructure.provider.RefreshTokenProvider;
import com.symphonia.member.domain.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthHelper {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenProvider refreshTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public String generateAccessToken(String memberId, String role) {
        return accessTokenProvider.generate(memberId, role);
    }

    public String bearerHeader(String accessToken) {
        return BEARER_PREFIX + accessToken;
    }

    public String bearerTokenFor(Member member) {
        String accessToken =
                generateAccessToken(String.valueOf(member.getId()), member.getRole().name());

        return bearerHeader(accessToken);
    }

    public String issueRefreshTokenFor(Member member) {
        String refreshToken = refreshTokenProvider.generate();
        refreshTokenRepository.save(
                refreshToken,
                String.valueOf(member.getId()),
                refreshTokenProvider.getExpirationTime());

        return refreshToken;
    }
}
