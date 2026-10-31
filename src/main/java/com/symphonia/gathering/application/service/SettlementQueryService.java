package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.QueryService;
import com.symphonia.gathering.application.dto.result.PayerSharesResult;
import com.symphonia.gathering.application.dto.result.SettlementFormResult;
import com.symphonia.gathering.application.dto.result.SettlementParticipantResult;
import com.symphonia.gathering.application.dto.result.SettlementResult;
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
import com.symphonia.gathering.domain.repository.ParticipantBankAccountRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.BankAccount;
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
    private final ParticipantBankAccountRepository participantBankAccountRepository;
    private final SettlementRepository settlementRepository;

    @Override
    public SettlementFormResult getForm(String linkToken) {
        Long participantId = findParticipantId(linkToken);
        Gathering gathering = findGatheringByParticipantId(participantId);
        boolean recordable =
                GatheringPolicy.isWithinRecordingPeriod(
                        gathering.getCreatedAt(), LocalDateTime.now());
        List<Long> defaultParticipantIds =
                settlementRepository
                        .findLatestByGatheringId(gathering.getId())
                        .map(Settlement::getParticipantIds)
                        .orElseGet(
                                () ->
                                        gathering.getParticipants().stream()
                                                .map(Participant::getId)
                                                .toList());

        return SettlementFormResult.of(
                recordable,
                participantBankAccountRepository.existsByParticipantId(participantId),
                SettlementPolicy.BANK_ACCOUNT_CONSENT_VERSION,
                gathering.getParticipants().stream()
                        .map(SettlementParticipantResult::from)
                        .toList(),
                defaultParticipantIds,
                settlementRepository.findAllByPayerParticipantId(participantId).stream()
                        .map(SettlementResult::from)
                        .toList());
    }

    // 결제자별로 묶고 결제자 묶음은 처음 기록한 정산 건의 순서를 따른다. 보낼 분담금이 남은 결제자를 앞에 둔다.
    // 결제자 계좌는 결제자마다 따로 읽지 않도록 한 번에 읽는다.
    @Override
    public List<PayerSharesResult> getShares(String linkToken) {
        Long participantId = findParticipantId(linkToken);
        Gathering gathering = findGatheringByParticipantId(participantId);
        List<Settlement> settlements =
                settlementRepository.findAllByShareParticipantId(participantId);
        if (settlements.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Settlement>> settlementsByPayerId =
                settlements.stream()
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

        return settlementsByPayerId.entrySet().stream()
                .map(
                        entry ->
                                PayerSharesResult.of(
                                        gathering.getParticipant(entry.getKey()),
                                        findPayerBankAccount(bankAccountsByPayerId, entry.getKey()),
                                        entry.getValue(),
                                        participantId))
                .sorted(Comparator.comparing(result -> !result.hasUnpaidShares()))
                .toList();
    }

    private Long findParticipantId(String linkToken) {
        return participantLinkTokenRepository
                .findParticipantIdByValue(linkToken)
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
