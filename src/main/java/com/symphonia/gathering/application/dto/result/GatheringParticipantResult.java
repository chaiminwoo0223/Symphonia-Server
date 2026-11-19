package com.symphonia.gathering.application.dto.result;

import com.symphonia.gathering.domain.entity.Participant;

// 모임장이 참여자를 고르는 데 쓰므로 휴대폰은 마스킹한 값만 담고, 개인 링크 토큰은 담지 않는다.
public record GatheringParticipantResult(Long id, String name, String maskedPhoneNumber) {
    public static GatheringParticipantResult from(Participant participant) {
        return new GatheringParticipantResult(
                participant.getId(), participant.getName(), participant.getPhoneNumber().masked());
    }
}
