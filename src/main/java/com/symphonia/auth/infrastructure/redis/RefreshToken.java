package com.symphonia.auth.infrastructure.redis;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@RedisHash("refresh_token")
public class RefreshToken {

    @Id
    private String memberId;

    private String value;

    @TimeToLive
    private Long expirationTime;

    public static RefreshToken of(String memberId, String value, Long expirationTime) {
        return RefreshToken.builder()
                .memberId(memberId)
                .value(value)
                .expirationTime(expirationTime)
                .build();
    }
}
