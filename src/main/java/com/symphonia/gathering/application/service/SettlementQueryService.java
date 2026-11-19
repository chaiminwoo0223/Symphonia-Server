package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.QueryService;
import com.symphonia.gathering.application.dto.query.GetSettlementFormQuery;
import com.symphonia.gathering.application.dto.result.GatheringRoundResult;
import com.symphonia.gathering.application.dto.result.LinkedParticipantResult;
import com.symphonia.gathering.application.dto.result.PayerCollectionResult;
import com.symphonia.gathering.application.dto.result.PayerSharesResult;
import com.symphonia.gathering.application.dto.result.SettlementFormResult;
import com.symphonia.gathering.application.dto.result.SettlementParticipantResult;
import com.symphonia.gathering.application.dto.result.SettlementResult;
import com.symphonia.gathering.application.dto.result.SharesResult;
import com.symphonia.gathering.application.usecase.GetLinkedParticipantUseCase;
import com.symphonia.gathering.application.usecase.GetPayerCollectionUseCase;
import com.symphonia.gathering.application.usecase.GetSettlementFormUseCase;
import com.symphonia.gathering.application.usecase.GetSharesUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.GatheringRetention;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@QueryService
@RequiredArgsConstructor
public class SettlementQueryService
        implements GetSettlementFormUseCase, GetSharesUseCase, GetPayerCollectionUseCase {
    private final GetLinkedParticipantUseCase getLinkedParticipantUseCase;
    private final GatheringRepository gatheringRepository;
    private final GatheringRetentionRepository gatheringRetentionRepository;
    private final SettlementRepository settlementRepository;
    private final Clock clock;

    // 모임을 만든 회원이 정산 건을 기록하는 화면이다. 모임의 취소하지 않은 정산 건을 모두 담는다.
    @Override
    public SettlementFormResult getForm(GetSettlementFormQuery query) {
        LocalDateTime now = LocalDateTime.now(clock);
        GatheringRetention retention =
                gatheringRetentionRepository
                        .findByGatheringId(query.gatheringId())
                        .filter(found -> !found.isExpired(now))
                        .orElseThrow(GatheringNotFoundException::new);
        // 정산 건을 모임보다 먼저 읽는다. 읽은 정산 건의 차수는 그 정산 건을 저장하기 전에 커밋되어 있고 차수는 지우지 않으므로,
        // 뒤에 읽는 모임에는 그 차수가 반드시 들어 있다.
        List<Settlement> settlements =
                settlementRepository.findAllByGatheringId(query.gatheringId()).stream()
                        .filter(settlement -> !settlement.isCancelled())
                        .toList();
        Gathering gathering = findGathering(query.gatheringId());
        GatheringPolicy.validateHost(gathering.getHostMemberId(), query.memberId());
        boolean recordable = GatheringPolicy.isWithinRecordingPeriod(gathering.getOpenedAt(), now);
        // settlements는 기록한 순서이므로 마지막 정산 건이 직전에 기록한 정산 건이다.
        List<Long> defaultParticipantIds =
                settlements.isEmpty()
                        ? gathering.getParticipants().stream().map(Participant::getId).toList()
                        : settlements.getLast().getParticipantIds();

        return SettlementFormResult.of(
                recordable,
                gathering.getRounds().stream().map(GatheringRoundResult::from).toList(),
                gathering.getParticipants().stream()
                        .map(SettlementParticipantResult::from)
                        .toList(),
                defaultParticipantIds,
                settlements.stream()
                        .map(
                                settlement ->
                                        SettlementResult.of(
                                                settlement,
                                                gathering.getRound(settlement.getRoundId())))
                        .toList(),
                retention.deletesAt());
    }

    // 결제자별로 묶고 결제자 묶음은 처음 기록한 정산 건의 순서를 따른다. 보낼 분담금이 남은 결제자를 앞에 둔다.
    @Override
    public SharesResult getShares(String linkToken) {
        LinkedParticipantResult linkedParticipant =
                getLinkedParticipantUseCase.getLinkedParticipant(linkToken);
        Long participantId = linkedParticipant.participantId();
        // 정산 건을 모임보다 먼저 읽는다.
        // 읽은 정산 건의 결제자는 그 정산 건을 저장하기 전에 참여했고 정산 건에 든 참여자는 지우지 않으므로, 뒤에 읽는 모임에는 그 결제자가 반드시 들어 있다.
        List<Settlement> shareSettlements =
                settlementRepository.findAllByShareParticipantId(participantId);
        Gathering gathering = findGatheringByParticipantId(participantId);
        // 차수의 참여자 이름은 모임의 취소하지 않은 정산 건 인원으로 구한다. 모임을 읽은 뒤 참여한 사람은 이름을 구할 수 없어 건너뛴다.
        List<Settlement> gatheringSettlements =
                settlementRepository.findAllByGatheringId(gathering.getId());
        Map<Long, List<Settlement>> settlementsByPayerId =
                shareSettlements.stream()
                        .collect(
                                Collectors.groupingBy(
                                        Settlement::getPayerParticipantId,
                                        LinkedHashMap::new,
                                        Collectors.toList()));

        List<PayerSharesResult> payers =
                settlementsByPayerId.entrySet().stream()
                        .map(
                                entry ->
                                        PayerSharesResult.of(
                                                gathering.getParticipant(entry.getKey()),
                                                entry.getValue(),
                                                participantId,
                                                gathering,
                                                gatheringSettlements))
                        .sorted(Comparator.comparing(result -> !result.hasUnpaidShares()))
                        .toList();

        return SharesResult.of(linkedParticipant.deletesAt(), payers);
    }

    // 결제자는 모임장이 지정한 참여자이고 개인 링크 토큰으로 요청한다. 정산 건을 모임보다 먼저 읽는다.
    // 읽은 정산 건의 참여자는 그 정산 건을 저장하기 전에 참여했고 정산 건에 든 참여자는 지우지 않으므로, 뒤에 읽는 모임에는 그 참여자가 반드시 들어 있다.
    @Override
    public PayerCollectionResult getCollection(String linkToken) {
        LinkedParticipantResult linkedParticipant =
                getLinkedParticipantUseCase.getLinkedParticipant(linkToken);
        Long payerParticipantId = linkedParticipant.participantId();
        List<Settlement> payerSettlements =
                settlementRepository.findAllByPayerParticipantId(payerParticipantId);
        Gathering gathering = findGatheringByParticipantId(payerParticipantId);

        return PayerCollectionResult.of(linkedParticipant.deletesAt(), gathering, payerSettlements);
    }

    private Gathering findGathering(Long gatheringId) {
        return gatheringRepository
                .findById(gatheringId)
                .orElseThrow(GatheringNotFoundException::new);
    }

    private Gathering findGatheringByParticipantId(Long participantId) {
        return gatheringRepository
                .findByParticipantId(participantId)
                .orElseThrow(GatheringNotFoundException::new);
    }
}
