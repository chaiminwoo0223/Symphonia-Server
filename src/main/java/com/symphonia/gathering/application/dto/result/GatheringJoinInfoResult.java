package com.symphonia.gathering.application.dto.result;

public record GatheringJoinInfoResult(String name, boolean joinable, String consentVersion) {
    public static GatheringJoinInfoResult of(String name, boolean joinable, String consentVersion) {
        return new GatheringJoinInfoResult(name, joinable, consentVersion);
    }
}
