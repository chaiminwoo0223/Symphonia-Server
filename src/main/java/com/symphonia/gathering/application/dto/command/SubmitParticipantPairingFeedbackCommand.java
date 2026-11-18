package com.symphonia.gathering.application.dto.command;

import com.symphonia.gathering.domain.vo.GatheringPairing;
import com.symphonia.pairing.application.dto.command.SubmitAnonymousPairingFeedbackCommand;
import com.symphonia.pairing.domain.vo.PairingRating;

// 참여자는 평점만 고른다. 관계와 분위기는 모임에서 고른 페어링의 값을 쓴다.
// drinkId와 anjuId는 참여자가 본 조합이다. 모임의 페어링이 그 사이 바뀌었는지 가리는 데 쓴다.
public record SubmitParticipantPairingFeedbackCommand(
        String linkToken, Long drinkId, Long anjuId, PairingRating rating) {
    public SubmitAnonymousPairingFeedbackCommand toSubmitAnonymousPairingFeedbackCommand(
            GatheringPairing gatheringPairing) {
        return SubmitAnonymousPairingFeedbackCommand.of(
                gatheringPairing.drinkId(),
                gatheringPairing.anjuId(),
                gatheringPairing.relationshipType(),
                gatheringPairing.moodType(),
                rating);
    }
}
