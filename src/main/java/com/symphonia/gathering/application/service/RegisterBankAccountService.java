package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.gathering.application.dto.command.RegisterBankAccountCommand;
import com.symphonia.gathering.application.usecase.GetLinkedParticipantUseCase;
import com.symphonia.gathering.application.usecase.RegisterBankAccountUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.ParticipantBankAccount;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.policy.SettlementPolicy;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.ParticipantBankAccountRepository;
import com.symphonia.gathering.domain.vo.BankAccount;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

// 결제자가 처음 정산 건을 기록하기 전에 개인 링크 토큰으로 자기 계좌를 등록한다. 요청한 참여자가 곧 계좌 주인이다.
@CommandService
@RequiredArgsConstructor
public class RegisterBankAccountService implements RegisterBankAccountUseCase {
    private final GetLinkedParticipantUseCase getLinkedParticipantUseCase;
    private final GatheringRepository gatheringRepository;
    private final ParticipantBankAccountRepository participantBankAccountRepository;
    private final Clock clock;

    @Override
    public void register(RegisterBankAccountCommand command) {
        Long participantId =
                getLinkedParticipantUseCase
                        .getLinkedParticipant(command.linkToken())
                        .participantId();
        Gathering gathering =
                gatheringRepository
                        .findByParticipantId(participantId)
                        .orElseThrow(GatheringNotFoundException::new);
        LocalDateTime now = LocalDateTime.now(clock);
        SettlementPolicy.validateRecordable(gathering.getOpenedAt(), now);
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
}
