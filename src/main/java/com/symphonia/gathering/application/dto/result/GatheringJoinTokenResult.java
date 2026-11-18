package com.symphonia.gathering.application.dto.result;

import java.time.LocalDateTime;

// expiresAt은 참여 토큰으로 참여할 수 있는 마지막 시각이다. 이 시각부터는 참여할 수 없다.
public record GatheringJoinTokenResult(
        Long gatheringId, String joinToken, LocalDateTime expiresAt) {
    public static GatheringJoinTokenResult of(
            Long gatheringId, String joinToken, LocalDateTime expiresAt) {
        return new GatheringJoinTokenResult(gatheringId, joinToken, expiresAt);
    }
}
