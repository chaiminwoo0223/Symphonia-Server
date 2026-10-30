package com.symphonia.gathering.presentation;

// 토큰은 접근 로그와 프록시에 남지 않도록 경로가 아니라 헤더로 받는다.
public final class GatheringHeaders {
    private GatheringHeaders() {}

    public static final String JOIN_TOKEN = "X-Join-Token";
    public static final String LINK_TOKEN = "X-Link-Token";
}
