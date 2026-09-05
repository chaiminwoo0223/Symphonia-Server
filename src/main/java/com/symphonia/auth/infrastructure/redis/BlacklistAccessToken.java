package com.symphonia.auth.infrastructure.redis;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@RedisHash("blacklist_access_token")
public class BlacklistAccessToken {

  @Id private String accessToken;

  private String memberId;

  @TimeToLive private Long expirationTime;

  public static BlacklistAccessToken of(String accessToken, String memberId, Long expirationTime) {
    return BlacklistAccessToken.builder()
        .accessToken(accessToken)
        .memberId(memberId)
        .expirationTime(expirationTime)
        .build();
  }
}
