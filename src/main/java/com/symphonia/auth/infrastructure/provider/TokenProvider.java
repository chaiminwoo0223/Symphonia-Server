package com.symphonia.auth.infrastructure.provider;

import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.global.config.properties.TokenProperties;
import com.symphonia.global.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

// 모바일: Response Body(JSON) → iOS Keychain / Android Keystore 저장
// 클라이언트: Response Body(JSON), HttpOnly Secure Cookie(BFF 패턴) → XSS 공격 / CSRF 공격 차단
@Component
public class TokenProvider {
    private final TokenProperties tokenProperties;
    private final SecretKey secretKey;

    public TokenProvider(TokenProperties tokenProperties) {
        this.tokenProperties = tokenProperties;
        this.secretKey = Keys.hmacShaKeyFor(tokenProperties.secret().getBytes());
    }

    private static final String ROLE_CLAIM = "role";

    public String generateAccessToken(String memberId, String role) {
        return generateToken(memberId, role, tokenProperties.accessExpirationTime());
    }

    public String getMemberId(String accessToken) {
        return parseClaims(accessToken).getSubject();
    }

    public String getRole(String accessToken) {
        return parseClaims(accessToken).get(ROLE_CLAIM, String.class);
    }

    public long getAccessTokenExpirationTime() {
        return tokenProperties.accessExpirationTime();
    }

    public long getRefreshTokenExpirationTime() {
        return tokenProperties.refreshExpirationTime();
    }

    private String generateToken(String memberId, String role, long expirationSeconds) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expirationSeconds);

        return Jwts.builder()
                .subject(memberId)
                .claim(ROLE_CLAIM, role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public boolean validate(String accessToken) {
        try {
            parse(accessToken);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        try {
            return parse(token).getPayload();
        } catch (Exception e) {
            throw BusinessException.from(AuthErrorCode.INVALID_JWT_TOKEN);
        }
    }

    private Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
    }
}
