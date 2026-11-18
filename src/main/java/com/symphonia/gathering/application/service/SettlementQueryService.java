package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.QueryService;
import com.symphonia.gathering.application.dto.result.GatheringRoundResult;
import com.symphonia.gathering.application.dto.result.LinkedParticipantResult;
import com.symphonia.gathering.application.dto.result.PayerSharesResult;
import com.symphonia.gathering.application.dto.result.SettlementFormResult;
import com.symphonia.gathering.application.dto.result.SettlementParticipantResult;
import com.symphonia.gathering.application.dto.result.SettlementResult;
import com.symphonia.gathering.application.dto.result.SharesResult;
import com.symphonia.gathering.application.usecase.GetLinkedParticipantUseCase;
import com.symphonia.gathering.application.usecase.GetSettlementFormUseCase;
import com.symphonia.gathering.application.usecase.GetSharesUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.entity.ParticipantBankAccount;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.policy.SettlementPolicy;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.ParticipantBankAccountRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.BankAccount;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@QueryService
@RequiredArgsConstructor
public class SettlementQueryService implements GetSettlementFormUseCase, GetSharesUseCase {
    private final GetLinkedParticipantUseCase getLinkedParticipantUseCase;
    private final GatheringRepository gatheringRepository;
    private final ParticipantBankAccountRepository participantBankAccountRepository;
    private final SettlementRepository settlementRepository;
    private final Clock clock;

    @Override
    public SettlementFormResult getForm(String linkToken) {
        LocalDateTime now = LocalDateTime.now(clock);
        LinkedParticipantResult linkedParticipant =
                getLinkedParticipantUseCase.getLinkedParticipant(linkToken);
        Long participantId = linkedParticipant.participantId();
        // 정산 건을 모임보다 먼저 읽는다. 읽은 정산 건의 차수는 그 정산 건을 저장하기 전에 커밋되어 있고 차수는 지우지 않으므로,
        // 뒤에 읽는 모임에는 그 차수가 반드시 들어 있다.
        List<Settlement> payerSettlements =
                settlementRepository.findAllByPayerParticipantId(participantId);
        Gathering gathering = findGatheringByParticipantId(participantId);
        boolean recordable = GatheringPolicy.isWithinRecordingPeriod(gathering.getOpenedAt(), now);
        List<Long> defaultParticipantIds =
                settlementRepository
                        .findLatestByGatheringId(gathering.getId())
                        .map(Settlement::getParticipantIds)
                        .map(
                                latestParticipantIds ->
                                        SettlementPolicy.getDefaultParticipantIds(
                                                latestParticipantIds, participantId))
                        .orElseGet(
                                () ->
                                        gathering.getParticipants().stream()
                                                .map(Participant::getId)
                                                .toList());

        return SettlementFormResult.of(
                recordable,
                participantBankAccountRepository.existsByParticipantId(participantId),
                SettlementPolicy.BANK_ACCOUNT_CONSENT_VERSION,
                gathering.getRounds().stream().map(GatheringRoundResult::from).toList(),
                gathering.getParticipants().stream()
                        .map(SettlementParticipantResult::from)
                        .toList(),
                defaultParticipantIds,
                payerSettlements.stream()
                        .map(
                                settlement ->
                                        SettlementResult.of(
                                                settlement,
                                                gathering.getRound(settlement.getRoundId())))
                        .toList(),
                linkedParticipant.deletesAt());
    }

    // 결제자별로 묶고 결제자 묶음은 처음 기록한 정산 건의 순서를 따른다. 보낼 분담금이 남은 결제자를 앞에 둔다.
    // 결제자 계좌는 결제자마다 따로 읽지 않도록 한 번에 읽는다.
    @Override
    public SharesResult getShares(String linkToken) {
        LinkedParticipantResult linkedParticipant =
                getLinkedParticipantUseCase.getLinkedParticipant(linkToken);
        Long participantId = linkedParticipant.participantId();
        // 정산 건을 모임보다 먼저 읽는다. 읽은 정산 건의 결제자는 그 정산 건을 저장하기 전에 참여했고 참여자는 지우지 않으므로,
        // 뒤에 읽는 모임에는 그 결제자가 반드시 들어 있다.
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
        Map<Long, BankAccount> bankAccountsByPayerId =
                participantBankAccountRepository
                        .findAllByParticipantIds(settlementsByPayerId.keySet(), gathering.getId())
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        ParticipantBankAccount::getParticipantId,
                                        ParticipantBankAccount::getBankAccount));

        List<PayerSharesResult> payers =
                settlementsByPayerId.entrySet().stream()
                        .map(
                                entry ->
                                        PayerSharesResult.of(
                                                gathering.getParticipant(entry.getKey()),
                                                findPayerBankAccount(
                                                        bankAccountsByPayerId, entry.getKey()),
                                                entry.getValue(),
                                                participantId,
                                                gathering,
                                                gatheringSettlements))
                        .sorted(Comparator.comparing(result -> !result.hasUnpaidShares()))
                        .toList();

        return SharesResult.of(linkedParticipant.deletesAt(), payers);
    }

    private Gathering findGatheringByParticipantId(Long participantId) {
        return gatheringRepository
                .findByParticipantId(participantId)
                .orElseThrow(GatheringNotFoundException::new);
    }

    // 결제자는 계좌를 등록해야 정산 건을 기록할 수 있고 계좌는 모임을 파기할 때만 지우므로, 없으면 서버 버그라서 500으로 둔다.
    private BankAccount findPayerBankAccount(
            Map<Long, BankAccount> bankAccountsByPayerId, Long payerParticipantId) {
        return Optional.ofNullable(bankAccountsByPayerId.get(payerParticipantId)).orElseThrow();
    }
}
