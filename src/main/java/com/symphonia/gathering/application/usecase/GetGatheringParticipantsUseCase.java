package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.query.GetGatheringParticipantsQuery;
import com.symphonia.gathering.application.dto.result.GatheringParticipantResult;
import java.util.List;

public interface GetGatheringParticipantsUseCase {
    List<GatheringParticipantResult> getParticipants(GetGatheringParticipantsQuery query);
}
