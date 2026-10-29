package com.symphonia.gathering.presentation;

public final class GatheringEndpoints {
    private GatheringEndpoints() {}

    private static final String BASE = "/api/v1/gatherings";

    // 인증된 회원이 모임을 만들고, 모임을 만든 회원이 참여 토큰을 재발급한다.
    public static final String CREATE = BASE;
    public static final String REISSUE = BASE + "/{gatheringId}/reissue";

    // 인증 없이 참여 QR의 참여 토큰으로 모임을 미리 보고 참여한다.
    public static final String PREVIEW = BASE + "/preview/{joinToken}";
    public static final String JOIN = BASE + "/join/{joinToken}";
}
