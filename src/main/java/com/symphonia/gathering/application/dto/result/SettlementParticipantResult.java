package com.symphonia.gathering.application.dto.result;

import com.symphonia.gathering.domain.entity.Participant;

// 결제자가 인원을 고르고 입금 현황을 보는 데 필요한 값만 담는다.
// 이름이 같은 참여자를 구분할 수 있도록 휴대폰을 마스킹해 담는다.
public record SettlementParticipantResult(Long id, String name, String maskedPhoneNumber) {
    public static SettlementParticipantResult from(Participant participant) {
        return new SettlementParticipantResult(
                participant.getId(), participant.getName(), participant.getPhoneNumber().masked());
    }
}
