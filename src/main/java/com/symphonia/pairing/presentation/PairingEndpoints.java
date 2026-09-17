package com.symphonia.pairing.presentation;

public final class PairingEndpoints {
    private PairingEndpoints() {}

    private static final String BASE = "/api/v1/pairings";

    public static final String RECOMMEND = BASE + "/recommend";
    public static final String FEEDBACK = BASE + "/feedback";
}
