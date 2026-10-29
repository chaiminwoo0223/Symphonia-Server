package com.symphonia.gathering.application.dto.result;

import com.symphonia.gathering.domain.entity.Participant;

// 휴대폰은 마스킹한 값만 담는다. 개인 링크 토큰은 원본을 다시 보여 줄 수 없어 참여 응답에서 한 번만 내보낸다.
public record ParticipantResult(
        Long id, String name, String maskedPhoneNumber, boolean nonDrinker, String linkToken) {
    public static ParticipantResult of(Participant participant, String linkToken) {
        return new ParticipantResult(
                participant.getId(),
                participant.getName(),
                participant.getPhoneNumber().masked(),
                participant.isNonDrinker(),
                linkToken);
    }
}
