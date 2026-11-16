package com.symphonia.gathering.application.dto.result;

import com.symphonia.gathering.domain.entity.GatheringRound;

public record GatheringRoundResult(Long id, int number) {
    public static GatheringRoundResult from(GatheringRound round) {
        return new GatheringRoundResult(round.getId(), round.getNumber());
    }
}
