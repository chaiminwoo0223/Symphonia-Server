package com.symphonia.gathering.presentation;

public final class SettlementEndpoints {
    private SettlementEndpoints() {}

    private static final String BASE = "/api/v1/settlements";

    // 인증 없이 X-Link-Token 헤더의 개인 링크 토큰으로 요청한다. 정산 건을 기록한 참여자가 그 정산 건의 결제자다.
    // 취소는 행을 지우지 않고 규칙이 있는 상태 전이라서 DELETE 대신 도메인 명령으로 드러낸다.
    public static final String FORM = BASE + "/form";
    public static final String RECORD = BASE;
    public static final String UPDATE = BASE + "/{settlementId}";
    public static final String CANCEL = UPDATE + "/cancel";
}
