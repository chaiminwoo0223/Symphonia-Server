package com.symphonia.gathering.presentation;

public final class GatheringEndpoints {
    private GatheringEndpoints() {}

    private static final String BASE = "/api/v1/gatherings";

    // 인증된 회원이 모임을 열고, 모임을 연 회원이 참여 토큰을 재발급한다.
    public static final String OPEN = BASE;
    public static final String REISSUE = BASE + "/{gatheringId}/join-token";

    // 인증 없이 X-Join-Token 헤더의 참여 토큰으로 모임을 미리 본다.
    public static final String PREVIEW = BASE + "/preview";
}
