package com.symphonia.auth.infrastructure.redis;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@RedisHash("withdrawn_member")
public class WithdrawnMember {
    @Id private String memberId;

    @TimeToLive private Long expirationTime;

    public static WithdrawnMember of(String memberId, Long expirationTime) {
        return WithdrawnMember.builder().memberId(memberId).expirationTime(expirationTime).build();
    }
}
