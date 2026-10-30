package com.symphonia.gathering.application.dto.result;

import com.symphonia.gathering.domain.entity.Participant;

// 결제자가 인원을 고르는 데 필요한 값만 담는다. 휴대폰은 담지 않는다.
public record SettlementParticipantResult(Long id, String name) {
    public static SettlementParticipantResult from(Participant participant) {
        return new SettlementParticipantResult(participant.getId(), participant.getName());
    }
}
