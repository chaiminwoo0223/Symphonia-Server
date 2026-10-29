package com.symphonia.gathering.application.dto.command;

public record JoinGatheringCommand(
        String joinToken,
        String name,
        String phoneNumber,
        boolean nonDrinker,
        String consentVersion) {}
