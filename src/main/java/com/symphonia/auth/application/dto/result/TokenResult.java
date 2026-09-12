package com.symphonia.auth.application.dto.result;

import com.symphonia.common.audit.HasActorId;

public record TokenResult(String accessToken, String refreshToken, String memberId)
        implements HasActorId {
    public static TokenResult of(String accessToken, String refreshToken, String memberId) {
        return new TokenResult(accessToken, refreshToken, memberId);
    }

    @Override
    public String actorId() {
        return memberId;
    }
}
