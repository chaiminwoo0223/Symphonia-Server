package com.symphonia.auth.infrastructure.provider;

import com.symphonia.auth.domain.error.AuthErrorCode;
import com.symphonia.global.config.properties.AccessTokenProperties;
import com.symphonia.global.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

// 모바일: Response Body(JSON) → iOS Keychain / Android Keystore 저장
// 클라이언트: Response Body(JSON), HttpOnly Secure Cookie(BFF 패턴) → XSS 공격 / CSRF 공격 차단
@Component
public class AccessTokenProvider {
  private final AccessTokenProperties accessTokenProperties;
  private final SecretKey secretKey;

  public AccessTokenProvider(AccessTokenProperties accessTokenProperties) {
    this.accessTokenProperties = accessTokenProperties;
    this.secretKey = Keys.hmacShaKeyFor(accessTokenProperties.secret().getBytes());
  }

  private static final String ROLE_CLAIM = "role";

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

  public String getMemberId(String accessToken) {
    return parse(accessToken).getSubject();
  }

  public String getRole(String accessToken) {
    return parse(accessToken).get(ROLE_CLAIM, String.class);
  }

  public long getRemainingTime(String accessToken) {
    Date expiration = parse(accessToken).getExpiration();
    long remaining = expiration.getTime() - System.currentTimeMillis();

    return Math.max(remaining, 0) / 1000;
  }

  public boolean validate(String accessToken) {
    try {
      decode(accessToken);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private Claims parse(String accessToken) {
    try {
      return decode(accessToken).getPayload();
    } catch (Exception e) {
      throw BusinessException.from(AuthErrorCode.INVALID_JWT_TOKEN);
    }
  }

  private Jws<Claims> decode(String accessToken) {
    return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(accessToken);
  }
}
