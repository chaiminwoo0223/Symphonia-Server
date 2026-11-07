package com.symphonia.gathering.presentation;

public final class ParticipantEndpoints {
    private ParticipantEndpoints() {}

    private static final String BASE = "/api/v1/participants";

    // 인증 없이 X-Join-Token 헤더의 참여 토큰으로 모임에 참여한다.
    public static final String JOIN = BASE;

    // 인증 없이 X-Link-Token 헤더의 개인 링크 토큰으로 요청한다. 토큰이 가리키는 참여자의 계좌를 등록한다.
    public static final String REGISTER_BANK_ACCOUNT = BASE + "/bank-account";

    // 인증 없이 X-Link-Token 헤더의 개인 링크 토큰으로 요청한다. 토큰이 가리키는 참여자의 모임에서 고른 페어링을 조회하고 피드백을 남긴다.
    public static final String PAIRING = BASE + "/pairing";
    public static final String PAIRING_FEEDBACK = PAIRING + "/feedback";
}
