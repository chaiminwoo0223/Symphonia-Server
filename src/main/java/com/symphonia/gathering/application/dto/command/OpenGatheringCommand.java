package com.symphonia.gathering.application.dto.command;

import java.util.Optional;
import java.util.UUID;

// clientRequestId는 같은 열기 요청을 다시 보냈는지 가리는 클라이언트의 요청 id다.
// 추천 결과에서 바로 모임을 열면 pairing을 함께 받고, 그렇지 않으면 null이다.
public record OpenGatheringCommand(
        Long memberId, UUID clientRequestId, String name, GatheringPairingCommand pairing) {
    public Optional<GatheringPairingCommand> findPairing() {
        return Optional.ofNullable(pairing);
    }
}
