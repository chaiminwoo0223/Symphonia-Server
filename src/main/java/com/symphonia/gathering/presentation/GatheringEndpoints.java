package com.symphonia.gathering.presentation;

public final class GatheringEndpoints {
    private GatheringEndpoints() {}

    private static final String BASE = "/api/v1/gatherings";

    // 인증된 회원이 모임을 만들고 참여 토큰을 재발급하는 경로다.
    public static final String GATHERINGS = BASE;
    public static final String GATHERING_JOIN_TOKEN = BASE + "/{gatheringId}/join-token";

    // 인증 없이 참여 QR의 참여 토큰으로 접근하는 경로다.
    public static final String GATHERING_BY_JOIN_TOKEN = BASE + "/by-join-token/{joinToken}";
    public static final String PARTICIPANTS_BY_JOIN_TOKEN =
            GATHERING_BY_JOIN_TOKEN + "/participants";
}
