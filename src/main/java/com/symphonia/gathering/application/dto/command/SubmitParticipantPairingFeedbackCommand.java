package com.symphonia.gathering.application.dto.command;

import com.symphonia.gathering.domain.vo.GatheringPairing;
import com.symphonia.pairing.application.dto.command.SubmitAnonymousPairingFeedbackCommand;
import com.symphonia.pairing.domain.vo.PairingRating;

// 참여자는 평점만 고른다. 관계와 분위기는 모임에서 고른 페어링의 값을 쓴다.
public record SubmitParticipantPairingFeedbackCommand(String linkToken, PairingRating rating) {
    public SubmitAnonymousPairingFeedbackCommand toSubmitAnonymousPairingFeedbackCommand(
            GatheringPairing gatheringPairing) {
        return SubmitAnonymousPairingFeedbackCommand.of(
                gatheringPairing.drinkId(),
                gatheringPairing.anjuId(),
                gatheringPairing.musicMoodId(),
                gatheringPairing.relationshipType(),
                gatheringPairing.moodType(),
                rating);
    }
}
