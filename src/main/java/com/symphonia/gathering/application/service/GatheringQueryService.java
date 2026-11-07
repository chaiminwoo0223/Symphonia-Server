package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.QueryService;
import com.symphonia.gathering.application.dto.result.GatheringResult;
import com.symphonia.gathering.application.usecase.GetExpiredGatheringsUseCase;
import com.symphonia.gathering.application.usecase.GetGatheringUseCase;
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
public class GatheringQueryService implements GetGatheringUseCase, GetExpiredGatheringsUseCase {
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

    // 가장 빨리 파기되는 모임도 기록 기간이 끝나고 7일 뒤에 파기되므로, 그보다 늦게 연 모임은 읽지 않는다.
    @Override
    public List<Long> getExpiredGatheringIds() {
        LocalDateTime now = LocalDateTime.now(clock);

        return gatheringRetentionRepository
                .findAllOpenedUntil(GatheringRetention.latestExpirableOpenedAt(now))
                .stream()
                .filter(retention -> retention.isExpired(now))
                .map(GatheringRetention::gatheringId)
                .toList();
    }
}
