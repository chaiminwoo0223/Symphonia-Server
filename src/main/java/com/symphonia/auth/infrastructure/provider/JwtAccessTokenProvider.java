package com.symphonia.auth.infrastructure.provider;

import com.symphonia.auth.domain.exception.UnauthenticatedException;
import com.symphonia.auth.domain.provider.AccessTokenProvider;
import com.symphonia.global.config.properties.AccessTokenProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// 모바일: Response Body(JSON) → iOS Keychain / Android Keystore 저장
// 클라이언트: Response Body(JSON), HttpOnly Secure Cookie(BFF 패턴) → XSS 공격 / CSRF 공격 차단
@Slf4j
@Component
public class JwtAccessTokenProvider implements AccessTokenProvider {
    private final AccessTokenProperties accessTokenProperties;
    private final SecretKey secretKey;

    public JwtAccessTokenProvider(AccessTokenProperties accessTokenProperties) {
        this.accessTokenProperties = accessTokenProperties;
        this.secretKey = Keys.hmacShaKeyFor(accessTokenProperties.secret().getBytes());
    }

    private static final String ROLE_CLAIM = "role";

    @Override
    public String generate(String memberId, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenProperties.expirationTime());

        return Jwts.builder()
                .subject(memberId)
                .claim(ROLE_CLAIM, role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public String getMemberId(String accessToken) {
        return parse(accessToken).getSubject();
    }

    @Override
    public String getRole(String accessToken) {
        return parse(accessToken).get(ROLE_CLAIM, String.class);
    }

    @Override
    public long getRemainingTime(String accessToken) {
        Date expiration = parse(accessToken).getExpiration();
        long remaining = expiration.getTime() - System.currentTimeMillis();

        return Math.max(remaining, 0) / 1000;
    }

    @Override
    public boolean validate(String accessToken) {
        try {
            parse(accessToken);
            return true;
        } catch (UnauthenticatedException e) {
            return false;
        }
    }

    // jjwt가 토큰을 거절할 때 던지는 예외만 401로 바꾸고, 그 밖의 예외는 서버 버그로 보고 그대로 던진다.
    // 토큰과 클레임이 로그에 남지 않도록 실패 이유는 예외 이름만 남긴다.
    private Claims parse(String accessToken) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("[AccessToken] 검증 실패: {}", e.getClass().getSimpleName());
            throw new UnauthenticatedException();
        }
    }
}
