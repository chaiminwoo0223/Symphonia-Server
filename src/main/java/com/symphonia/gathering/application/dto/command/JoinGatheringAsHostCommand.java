package com.symphonia.gathering.application.dto.command;

public record JoinGatheringAsHostCommand(
        Long memberId,
        Long gatheringId,
        String name,
        String phoneNumber,
        boolean consentAgreed,
        String consentVersion) {}
