package com.symphonia.gathering.presentation;

public final class GatheringEndpoints {
    private GatheringEndpoints() {}

    private static final String BASE = "/api/v1/gatherings";

    // 인증된 회원이 모임을 열고, 모임을 연 회원이 참여 토큰을 재발급하거나 페어링을 고른다.
    public static final String OPEN = BASE;
    public static final String REISSUE = BASE + "/{gatheringId}/join-token";
    public static final String CHOOSE_PAIRING = BASE + "/{gatheringId}/pairing";

    // 모임을 연 회원이 참여자 목록을 보고, 참여자를 제거하고, 개인 링크를 재발급한다.
    public static final String PARTICIPANTS = BASE + "/{gatheringId}/participants";
    public static final String PARTICIPANT = PARTICIPANTS + "/{participantId}";
    public static final String REISSUE_LINK_TOKEN = PARTICIPANT + "/link-token";

    // 인증 없이 X-Join-Token 헤더의 참여 토큰으로 모임을 미리 본다.
    public static final String PREVIEW = BASE + "/preview";
}
