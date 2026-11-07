package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.QueryService;
import com.symphonia.gathering.application.dto.result.GatheringResult;
import com.symphonia.gathering.application.usecase.GetGatheringUseCase;
import com.symphonia.gathering.application.usecase.GetPurgeCandidatesUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.exception.JoinTokenNotFoundException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.repository.GatheringJoinTokenRepository;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.domain.vo.GatheringRetention;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;

@QueryService
@RequiredArgsConstructor
public class GatheringQueryService implements GetGatheringUseCase, GetPurgeCandidatesUseCase {
    private final GatheringRepository gatheringRepository;
    private final GatheringJoinTokenRepository gatheringJoinTokenRepository;
    private final GatheringRetentionRepository gatheringRetentionRepository;
    private final Clock clock;

    // 파기 시각이 지난 모임은 물리 삭제 전이라도 참여 토큰을 찾지 못한 것으로 본다. 삭제 뒤의 응답과 같게 하기 위해서다.
    @Override
    public GatheringResult getByJoinToken(String joinToken) {
        LocalDateTime now = LocalDateTime.now(clock);
        Long gatheringId =
                gatheringJoinTokenRepository
                        .findGatheringIdByValue(joinToken)
                        .orElseThrow(JoinTokenNotFoundException::new);
        GatheringRetention retention =
                gatheringRetentionRepository
                        .findByGatheringId(gatheringId)
                        .filter(found -> !found.isExpired(now))
                        .orElseThrow(JoinTokenNotFoundException::new);
        Gathering gathering =
                gatheringRepository
                        .findById(gatheringId)
                        .orElseThrow(GatheringNotFoundException::new);
        boolean joinable = GatheringPolicy.isWithinRecordingPeriod(gathering.getOpenedAt(), now);

        return GatheringResult.of(
                gathering.getName(),
                joinable,
                GatheringPolicy.CONSENT_VERSION,
                retention.deletesAt());
    }

    @Override
    public List<Long> getPurgeCandidateIds() {
        return gatheringRetentionRepository.findGatheringIdsOpenedUntil(
                GatheringRetention.latestExpirableOpenedAt(LocalDateTime.now(clock)));
    }
}
