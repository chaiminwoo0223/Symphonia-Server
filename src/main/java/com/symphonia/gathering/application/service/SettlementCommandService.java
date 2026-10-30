package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.gathering.application.dto.command.CancelSettlementCommand;
import com.symphonia.gathering.application.dto.command.RecordSettlementCommand;
import com.symphonia.gathering.application.dto.command.RegisterBankAccountCommand;
import com.symphonia.gathering.application.dto.command.UpdateSettlementCommand;
import com.symphonia.gathering.application.dto.result.SettlementResult;
import com.symphonia.gathering.application.usecase.CancelSettlementUseCase;
import com.symphonia.gathering.application.usecase.RecordSettlementUseCase;
import com.symphonia.gathering.application.usecase.RegisterBankAccountUseCase;
import com.symphonia.gathering.application.usecase.UpdateSettlementUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.ParticipantBankAccount;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.exception.ParticipantNotFoundException;
import com.symphonia.gathering.domain.exception.SettlementNotFoundException;
import com.symphonia.gathering.domain.policy.SettlementPolicy;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.ParticipantBankAccountRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.BankAccount;
import com.symphonia.gathering.domain.vo.SplitMethod;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

// 정산 건은 결제자가 개인 링크 토큰으로 기록하고 수정하고 취소한다. 요청한 참여자가 곧 결제자다.
@CommandService
@RequiredArgsConstructor
public class SettlementCommandService
        implements RegisterBankAccountUseCase,
                RecordSettlementUseCase,
                UpdateSettlementUseCase,
                CancelSettlementUseCase {
    private final ParticipantLinkTokenRepository participantLinkTokenRepository;
    private final GatheringRepository gatheringRepository;
    private final ParticipantBankAccountRepository participantBankAccountRepository;
    private final SettlementRepository settlementRepository;

    @Override
    public void register(RegisterBankAccountCommand command) {
        Long participantId = findParticipantId(command.linkToken());
        Gathering gathering = findGatheringByParticipantId(participantId);
        LocalDateTime now = LocalDateTime.now();
        SettlementPolicy.validateRecordable(gathering.getCreatedAt(), now);
        BankAccount bankAccount = BankAccount.of(command.bankName(), command.accountNumber());
        SettlementPolicy.validateBankAccountConsent(
                command.consentAgreed(), command.consentVersion());
        SettlementPolicy.validateBankAccountNotRegistered(
                participantBankAccountRepository.existsByParticipantId(participantId));

        ParticipantBankAccount participantBankAccount =
                ParticipantBankAccount.register(
                        participantId, bankAccount, command.consentVersion(), now);
        participantBankAccountRepository.save(participantBankAccount, gathering.getId());
    }

    @Override
    public SettlementResult record(RecordSettlementCommand command) {
        Long participantId = findParticipantId(command.linkToken());
        Gathering gathering = findGatheringByParticipantId(participantId);
        LocalDateTime now = LocalDateTime.now();
        SettlementPolicy.validateRecordable(gathering.getCreatedAt(), now);
        SettlementPolicy.validateGatheringParticipants(
                gathering.hasParticipants(command.participantIds()));
        SettlementPolicy.validateBankAccountRegistered(
                participantBankAccountRepository.existsByParticipantId(participantId));
        SplitMethod splitMethod =
                SplitMethod.of(
                        command.totalAmount(),
                        participantId,
                        command.participantIds(),
                        command.fixedAmounts());

        Settlement settlement = Settlement.record(gathering.getId(), splitMethod, now);
        Settlement savedSettlement = settlementRepository.save(settlement);

        return SettlementResult.from(savedSettlement);
    }

    @Override
    public SettlementResult update(UpdateSettlementCommand command) {
        Long participantId = findParticipantId(command.linkToken());
        Settlement settlement = findSettlement(command.settlementId());
        SettlementPolicy.validatePayer(settlement.getPayerParticipantId(), participantId);
        Gathering gathering =
                gatheringRepository
                        .findById(settlement.getGatheringId())
                        .orElseThrow(GatheringNotFoundException::new);
        LocalDateTime now = LocalDateTime.now();
        SettlementPolicy.validateRecordable(gathering.getCreatedAt(), now);
        SettlementPolicy.validateGatheringParticipants(
                gathering.hasParticipants(command.participantIds()));
        SplitMethod splitMethod =
                SplitMethod.of(
                        command.totalAmount(),
                        participantId,
                        command.participantIds(),
                        command.fixedAmounts());

        settlement.update(splitMethod, now);
        Settlement savedSettlement = settlementRepository.save(settlement);

        return SettlementResult.from(savedSettlement);
    }

    @Override
    public void cancel(CancelSettlementCommand command) {
        Long participantId = findParticipantId(command.linkToken());
        Settlement settlement = findSettlement(command.settlementId());
        SettlementPolicy.validatePayer(settlement.getPayerParticipantId(), participantId);

        settlement.cancel(LocalDateTime.now());
        settlementRepository.save(settlement);
    }

    private Long findParticipantId(String linkToken) {
        return participantLinkTokenRepository
                .findParticipantIdByValue(linkToken)
                .orElseThrow(ParticipantNotFoundException::new);
    }

    private Gathering findGatheringByParticipantId(Long participantId) {
        return gatheringRepository
                .findByParticipantId(participantId)
                .orElseThrow(GatheringNotFoundException::new);
    }

    private Settlement findSettlement(Long settlementId) {
        return settlementRepository
                .findById(settlementId)
                .orElseThrow(SettlementNotFoundException::new);
    }
}
