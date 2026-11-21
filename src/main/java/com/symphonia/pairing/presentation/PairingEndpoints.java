package com.symphonia.pairing.presentation;

public final class PairingEndpoints {
    private PairingEndpoints() {}

    private static final String BASE = "/api/v1/pairings";

    public static final String RECOMMEND = BASE + "/recommendations";
    // 회원이 피드백을 내고(POST), 자기가 낸 피드백을 본다(GET).
    public static final String FEEDBACK = BASE + "/feedback";
}
