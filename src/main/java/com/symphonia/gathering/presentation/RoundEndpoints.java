package com.symphonia.gathering.presentation;

public final class RoundEndpoints {
    private RoundEndpoints() {}

    private static final String BASE = "/api/v1/rounds";

    // 인증 없이 X-Link-Token 헤더의 개인 링크 토큰으로 요청한다. 토큰이 가리키는 참여자의 모임에 차수를 추가한다.
    public static final String ADD = BASE;
}
