package com.symphonia.auth.infrastructure.redis;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@RedisHash("refresh_token")
public class RefreshToken {

    @Id
    private String value;

    @Indexed // Spring Data Redis가 자동으로 역색인 key 추가 저장
    private String memberId;

    @TimeToLive
    private Long expirationTime;

    public static RefreshToken of(String value, String memberId, Long expirationTime) {
        return RefreshToken.builder()
                .value(value)
                .memberId(memberId)
                .expirationTime(expirationTime)
                .build();
    }
}
