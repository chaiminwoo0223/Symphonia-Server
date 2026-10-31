package com.symphonia.gathering.presentation;

public final class ShareEndpoints {
    private static final String BASE = "/api/v1/shares";

    private ShareEndpoints() {}

    // 인증 없이 X-Link-Token 헤더의 개인 링크 토큰으로 요청한다. 토큰이 가리키는 참여자가 결제자에게 낼 분담을 다룬다.
    public static final String LIST = BASE;
    public static final String SENT = BASE + "/sent";
}
