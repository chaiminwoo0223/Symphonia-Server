package com.symphonia.gathering.application.dto.result;

public record GatheringJoinTokenResult(Long gatheringId, String joinToken) {
    public static GatheringJoinTokenResult of(Long gatheringId, String joinToken) {
        return new GatheringJoinTokenResult(gatheringId, joinToken);
    }
}
