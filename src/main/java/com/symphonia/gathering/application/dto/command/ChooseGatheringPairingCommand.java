package com.symphonia.gathering.application.dto.command;

public record ChooseGatheringPairingCommand(
        Long memberId, Long gatheringId, GatheringPairingCommand pairing) {
    public static ChooseGatheringPairingCommand of(
            Long memberId, Long gatheringId, GatheringPairingCommand pairing) {
        return new ChooseGatheringPairingCommand(memberId, gatheringId, pairing);
    }
}
