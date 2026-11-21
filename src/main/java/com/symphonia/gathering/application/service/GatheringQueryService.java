package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.QueryService;
import com.symphonia.gathering.application.dto.query.GetGatheringParticipantsQuery;
import com.symphonia.gathering.application.dto.result.GatheringParticipantResult;
import com.symphonia.gathering.application.dto.result.GatheringResult;
import com.symphonia.gathering.application.dto.result.HostedGatheringResult;
import com.symphonia.gathering.application.dto.result.OngoingGatheringsResult;
import com.symphonia.gathering.application.usecase.GetExpiredGatheringsUseCase;
import com.symphonia.gathering.application.usecase.GetGatheringParticipantsUseCase;
import com.symphonia.gathering.application.usecase.GetGatheringUseCase;
import com.symphonia.gathering.application.usecase.GetHostedGatheringsUseCase;
import com.symphonia.gathering.application.usecase.GetOngoingGatheringsUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.exception.JoinTokenNotFoundException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.repository.GatheringJoinTokenRepository;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.domain.vo.GatheringRetention;
import com.symphonia.gathering.domain.vo.JoinClosedReason;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@QueryService
@RequiredArgsConstructor
public class GatheringQueryService
        implements GetGatheringUseCase,
                GetGatheringParticipantsUseCase,
                GetOngoingGatheringsUseCase,
                GetHostedGatheringsUseCase,
                GetExpiredGatheringsUseCase {
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
        GatheringRetention retention = findRetention(gatheringId, now);
        Gathering gathering =
                gatheringRepository
                        .findById(gatheringId)
                        .orElseThrow(GatheringNotFoundException::new);
        JoinClosedReason joinClosedReason =
                GatheringPolicy.findJoinClosedReason(
                                gathering.getRecordingClosesAt(),
                                now,
                                gathering.getParticipants().size())
                        .orElse(null);

        return GatheringResult.of(
                gathering.getName(),
                joinClosedReason,
                GatheringPolicy.CONSENT_VERSION,
                retention.deletesAt());
    }

    // 파기 시각이 지난 모임은 물리 삭제 전이라도 찾지 못한 것으로 본다. 삭제 뒤의 응답과 같게 하기 위해서다.
    @Override
    public List<GatheringParticipantResult> getParticipants(GetGatheringParticipantsQuery query) {
        LocalDateTime now = LocalDateTime.now(clock);
        gatheringRetentionRepository
                .findByGatheringId(query.gatheringId())
                .filter(retention -> !retention.isExpired(now))
                .orElseThrow(GatheringNotFoundException::new);
        Gathering gathering =
                gatheringRepository
                        .findById(query.gatheringId())
                        .orElseThrow(GatheringNotFoundException::new);
        GatheringPolicy.validateHost(gathering.getHostMemberId(), query.memberId());

        return gathering.getParticipants().stream().map(GatheringParticipantResult::from).toList();
    }

    // 모임장이 탈퇴하면 새 정산 건 기록과 참여자 관리가 멈추므로, 탈퇴하기 전에 진행 중인 모임을 알려 주는 데 쓴다.
    // 파기 시각이 지난 모임은 물리 삭제 전이라도 진행 중이 아닌 것으로 본다.
    @Override
    public OngoingGatheringsResult getOngoing(Long hostMemberId) {
        LocalDateTime now = LocalDateTime.now(clock);

        return OngoingGatheringsResult.from(
                gatheringRetentionRepository.findAllByHostMemberId(hostMemberId).stream()
                        .filter(retention -> !retention.isExpired(now))
                        .toList());
    }

    // 파기 시각이 지난 모임은 물리 삭제 전이라도 목록에 담지 않는다. 삭제 뒤의 응답과 같게 하기 위해서다.
    // 파기 시각은 정산 건으로 계산하므로 getOngoing과 같은 조회를 쓰고, 이름과 참여자 수는 그 모임 id로 따로 읽는다.
    // 연 시각, 기록 기간, 연장 여부는 파기 시각과 같은 조회에서 읽은 값을 쓴다. 두 조회 사이에 연장이 커밋돼도 응답의 값이 서로 어긋나지 않게 하기 위해서다.
    @Override
    public List<HostedGatheringResult> getHosted(Long hostMemberId) {
        LocalDateTime now = LocalDateTime.now(clock);
        Map<Long, GatheringRetention> retentionsByGatheringId =
                gatheringRetentionRepository.findAllByHostMemberId(hostMemberId).stream()
                        .filter(retention -> !retention.isExpired(now))
                        .collect(
                                Collectors.toMap(
                                        GatheringRetention::gatheringId, Function.identity()));

        return gatheringRepository
                .findAllHostedByIds(List.copyOf(retentionsByGatheringId.keySet()))
                .stream()
                .map(
                        hostedGathering ->
                                HostedGatheringResult.of(
                                        hostedGathering,
                                        retentionsByGatheringId.get(hostedGathering.gatheringId())))
                .toList();
    }

    // 가장 빨리 파기되는 모임도 기록 기간이 끝나고 7일 뒤에 파기되므로, 그보다 늦게 연 모임은 읽지 않는다.
    // 연장은 파기를 늦추기만 하므로 기준은 가장 짧은 기록 기간이고, 연장한 모임은 파기 시각 판단이 걸러 준다.
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

    private GatheringRetention findRetention(Long gatheringId, LocalDateTime now) {
        return gatheringRetentionRepository
                .findByGatheringId(gatheringId)
                .filter(retention -> !retention.isExpired(now))
                .orElseThrow(JoinTokenNotFoundException::new);
    }
}
