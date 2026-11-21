package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.result.OngoingGatheringsResult;

public interface GetOngoingGatheringsUseCase {
    OngoingGatheringsResult getOngoing(Long hostMemberId);
}
