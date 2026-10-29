package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.QueryService;
import com.symphonia.gathering.application.dto.result.GatheringJoinInfoResult;
import com.symphonia.gathering.application.usecase.GetGatheringJoinInfoUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.repository.GatheringJoinTokenRepository;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

@QueryService
@RequiredArgsConstructor
public class GatheringQueryService implements GetGatheringJoinInfoUseCase {
    private final GatheringRepository gatheringRepository;
    private final GatheringJoinTokenRepository gatheringJoinTokenRepository;

    @Override
    public GatheringJoinInfoResult getJoinInfo(String joinToken) {
        Long gatheringId =
                gatheringJoinTokenRepository
                        .findGatheringIdByValue(joinToken)
                        .orElseThrow(GatheringNotFoundException::new);
        Gathering gathering =
                gatheringRepository
                        .findById(gatheringId)
                        .orElseThrow(GatheringNotFoundException::new);
        boolean joinable =
                GatheringPolicy.isWithinRecordingPeriod(
                        gathering.getCreatedAt(), LocalDateTime.now());

        return GatheringJoinInfoResult.of(
                gathering.getName(), joinable, GatheringPolicy.CONSENT_VERSION);
    }
}
