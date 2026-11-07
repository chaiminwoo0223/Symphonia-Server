package com.symphonia.gathering.application.dto.command;

import java.util.Optional;

// 추천 결과에서 바로 모임을 열면 pairing을 함께 받고, 그렇지 않으면 null이다.
public record OpenGatheringCommand(Long memberId, String name, GatheringPairingCommand pairing) {
    public Optional<GatheringPairingCommand> findPairing() {
        return Optional.ofNullable(pairing);
    }
}
