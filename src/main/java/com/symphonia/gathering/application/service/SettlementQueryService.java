package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.QueryService;
import com.symphonia.gathering.application.dto.result.PayerSharesResult;
import com.symphonia.gathering.application.dto.result.SettlementFormResult;
import com.symphonia.gathering.application.dto.result.SettlementParticipantResult;
import com.symphonia.gathering.application.dto.result.SettlementResult;
import com.symphonia.gathering.application.dto.result.SharesResult;
import com.symphonia.gathering.application.usecase.GetSettlementFormUseCase;
import com.symphonia.gathering.application.usecase.GetSharesUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.entity.ParticipantBankAccount;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.exception.LinkTokenNotFoundException;
import com.symphonia.gathering.domain.policy.GatheringPolicy;
import com.symphonia.gathering.domain.policy.SettlementPolicy;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.domain.repository.ParticipantBankAccountRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.BankAccount;
import com.symphonia.gathering.domain.vo.GatheringRetention;
import com.symphonia.notification.application.usecase.GetNotificationScheduleUseCase;
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
    private final ParticipantLinkTokenRepository participantLinkTokenRepository;
    private final GatheringRepository gatheringRepository;
    private final GatheringRetentionRepository gatheringRetentionRepository;
    private final ParticipantBankAccountRepository participantBankAccountRepository;
    private final SettlementRepository settlementRepository;
    private final GetNotificationScheduleUseCase getNotificationScheduleUseCase;
    private final Clock clock;

    @Override
    public SettlementFormResult getForm(String linkToken) {
        LocalDateTime now = LocalDateTime.now(clock);
        Long participantId = findParticipantId(linkToken);
        GatheringRetention retention = findRetention(participantId, now);
        Gathering gathering = findGatheringByParticipantId(participantId);
        boolean recordable = GatheringPolicy.isWithinRecordingPeriod(gathering.getOpenedAt(), now);
        List<Long> defaultParticipantIds =
                settlementRepository
                        .findLatestByGatheringId(gathering.getId())
                        .map(Settlement::getParticipantIds)
                        .orElseGet(
                                () ->
                                        gathering.getParticipants().stream()
                                                .map(Participant::getId)
                                                .toList());

        LocalDateTime scheduledAt =
                getNotificationScheduleUseCase.getSchedule(gathering.getId()).scheduledAt();

        return SettlementFormResult.of(
                recordable,
                participantBankAccountRepository.existsByParticipantId(participantId),
                SettlementPolicy.BANK_ACCOUNT_CONSENT_VERSION,
                gathering.getParticipants().stream()
                        .map(SettlementParticipantResult::from)
                        .toList(),
                defaultParticipantIds,
                settlementRepository.findAllByPayerParticipantId(participantId).stream()
                        .map(settlement -> SettlementResult.of(settlement, scheduledAt))
                        .toList(),
                retention.deletesAt());
    }

    // 결제자별로 묶고 결제자 묶음은 처음 기록한 정산 건의 순서를 따른다. 보낼 분담금이 남은 결제자를 앞에 둔다.
    // 결제자 계좌는 결제자마다 따로 읽지 않도록 한 번에 읽는다.
    @Override
    public SharesResult getShares(String linkToken) {
        Long participantId = findParticipantId(linkToken);
        GatheringRetention retention = findRetention(participantId, LocalDateTime.now(clock));
        Gathering gathering = findGatheringByParticipantId(participantId);
        Map<Long, List<Settlement>> settlementsByPayerId =
                settlementRepository.findAllByShareParticipantId(participantId).stream()
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
                                                participantId))
                        .sorted(Comparator.comparing(result -> !result.hasUnpaidShares()))
                        .toList();

        return SharesResult.of(retention.deletesAt(), payers);
    }

    private Long findParticipantId(String linkToken) {
        return participantLinkTokenRepository
                .findParticipantIdByValue(linkToken)
                .orElseThrow(LinkTokenNotFoundException::new);
    }

    // 파기 시각이 지난 모임의 링크는 물리 삭제 전이라도 찾지 못한 것으로 본다. 삭제 뒤의 응답과 같게 하기 위해서다.
    // 데이터 키를 지운 모임을 복호화하지 않도록 모임을 읽기 전에 부른다.
    private GatheringRetention findRetention(Long participantId, LocalDateTime now) {
        return gatheringRetentionRepository
                .findByParticipantId(participantId)
                .filter(retention -> !retention.isExpired(now))
                .orElseThrow(LinkTokenNotFoundException::new);
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
