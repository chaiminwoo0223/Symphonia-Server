package com.symphonia.gathering.presentation;

public final class GatheringEndpoints {
    private GatheringEndpoints() {}

    private static final String BASE = "/api/v1/gatherings";

    // 인증된 회원이 모임을 열고, 모임을 연 회원이 참여 토큰을 재발급하거나 페어링을 고르거나 차수를 추가하거나 기록 기간을 연장한다.
    public static final String OPEN = BASE;
    public static final String REISSUE = BASE + "/{gatheringId}/join-token";
    public static final String CHOOSE_PAIRING = BASE + "/{gatheringId}/pairing";
    public static final String ADD_ROUND = BASE + "/{gatheringId}/rounds";
    public static final String EXTEND_RECORDING = BASE + "/{gatheringId}/recording-extension";

    // 모임을 연 회원이 정산 건을 기록하고, 고치고, 취소한다. 결제자는 기록할 때 지정한다.
    public static final String SETTLEMENTS = BASE + "/{gatheringId}/settlements";
    public static final String SETTLEMENT_FORM = SETTLEMENTS + "/form";
    public static final String SETTLEMENT = SETTLEMENTS + "/{settlementId}";
    // 취소는 행을 지우지 않고 규칙이 있는 상태 변경이라서 DELETE 대신 도메인 명령으로 드러낸다.
    public static final String CANCEL_SETTLEMENT = SETTLEMENT + "/cancel";

    // 모임을 연 회원이 참여자 목록을 보고, 참여자를 제거하고, 개인 링크를 재발급한다.
    public static final String PARTICIPANTS = BASE + "/{gatheringId}/participants";
    public static final String PARTICIPANT = PARTICIPANTS + "/{participantId}";
    public static final String REISSUE_LINK_TOKEN = PARTICIPANT + "/link-token";

    // 인증 없이 X-Join-Token 헤더의 참여 토큰으로 모임을 미리 본다.
    public static final String PREVIEW = BASE + "/preview";
}
