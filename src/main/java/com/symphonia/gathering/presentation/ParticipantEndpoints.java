package com.symphonia.gathering.presentation;

public final class ParticipantEndpoints {
    private ParticipantEndpoints() {}

    private static final String BASE = "/api/v1/participants";

    // 인증 없이 X-Join-Token 헤더의 참여 토큰으로 모임에 참여한다.
    public static final String JOIN = BASE;
}
