package com.symphonia.gathering.application.dto.result;

public record GatheringResult(String name, boolean joinable, String consentVersion) {
    public static GatheringResult of(String name, boolean joinable, String consentVersion) {
        return new GatheringResult(name, joinable, consentVersion);
    }
}
