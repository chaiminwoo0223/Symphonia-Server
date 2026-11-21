package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.result.HostedGatheringResult;
import java.util.List;

public interface GetHostedGatheringsUseCase {
    List<HostedGatheringResult> getHosted(Long hostMemberId);
}
