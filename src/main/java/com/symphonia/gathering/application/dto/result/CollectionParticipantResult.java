package com.symphonia.gathering.application.dto.result;

import com.symphonia.gathering.domain.entity.Participant;
import java.util.List;

// 결제자가 카카오톡 방에 초대할 수 있도록 참여자의 이름과 전체 휴대폰을 담는다. totalAmount는 shares의 금액을 모두 더한 값이다.
public record CollectionParticipantResult(
        Long participantId,
        String name,
        String phoneNumber,
        long totalAmount,
        List<CollectionShareResult> shares) {
    public static CollectionParticipantResult of(
            Participant participant, List<CollectionShareResult> shares) {
        return new CollectionParticipantResult(
                participant.getId(),
                participant.getName(),
                participant.getPhoneNumber().value(),
                shares.stream().mapToLong(CollectionShareResult::amount).sum(),
                shares);
    }

    // 로그나 예외 메시지에 전체 번호가 남지 않게 번호를 뺀다.
    @Override
    public String toString() {
        return "CollectionParticipantResult[participantId=" + participantId + "]";
    }
}
