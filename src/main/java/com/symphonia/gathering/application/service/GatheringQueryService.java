package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.QueryService;
import com.symphonia.gathering.application.dto.result.GatheringResult;
import com.symphonia.gathering.application.usecase.GetGatheringUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.exception.InvalidJoinTokenException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.repository.GatheringJoinTokenRepository;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

@QueryService
@RequiredArgsConstructor
public class GatheringQueryService implements GetGatheringUseCase {
    private final GatheringRepository gatheringRepository;
    private final GatheringJoinTokenRepository gatheringJoinTokenRepository;

    @Override
    public GatheringResult getByJoinToken(String joinToken) {
        Long gatheringId =
                gatheringJoinTokenRepository
                        .findGatheringIdByValue(joinToken)
                        .orElseThrow(InvalidJoinTokenException::new);
        Gathering gathering =
                gatheringRepository
                        .findById(gatheringId)
                        .orElseThrow(GatheringNotFoundException::new);
        boolean joinable =
                GatheringPolicy.isWithinRecordingPeriod(
                        gathering.getCreatedAt(), LocalDateTime.now());

        return GatheringResult.of(gathering.getName(), joinable, GatheringPolicy.CONSENT_VERSION);
    }
}
