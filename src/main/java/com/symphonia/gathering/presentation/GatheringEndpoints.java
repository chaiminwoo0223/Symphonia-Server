package com.symphonia.gathering.presentation;

public final class GatheringEndpoints {
    private GatheringEndpoints() {}

    private static final String BASE = "/api/v1/gatherings";

    public static final String GATHERINGS = BASE;
    public static final String JOIN_TOKEN = BASE + "/{gatheringId}/join-token";
    public static final String JOIN = BASE + "/join/{joinToken}";
}
