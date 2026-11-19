package com.symphonia.gathering.presentation;

public final class SettlementEndpoints {
    private static final String BASE = "/api/v1/settlements";

    private SettlementEndpoints() {}

    private static final String SETTLEMENT = BASE + "/{settlementId}";

    // 인증 없이 X-Link-Token 헤더의 개인 링크 토큰으로 요청한다. 모임장이 결제자로 지정한 참여자가 그 정산 건의 결제자다.
    // 결제자가 정산 건의 분담금 하나의 상태를 정정한다. 확인 완료는 보냄 확인과 직접 받은 돈 처리를 함께 다룬다.
    public static final String CONFIRM_SHARE = SETTLEMENT + "/shares/{participantId}/confirm";
    public static final String REVERT_SHARE = SETTLEMENT + "/shares/{participantId}/unpaid";
}
