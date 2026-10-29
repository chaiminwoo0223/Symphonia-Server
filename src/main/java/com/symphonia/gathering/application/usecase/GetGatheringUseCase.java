package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.result.GatheringResult;

public interface GetGatheringUseCase {
    GatheringResult getByJoinToken(String joinToken);
}
