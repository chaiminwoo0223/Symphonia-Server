package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.result.GatheringJoinInfoResult;

public interface GetGatheringJoinInfoUseCase {
    GatheringJoinInfoResult getJoinInfo(String joinToken);
}
