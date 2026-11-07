package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.gathering.application.dto.command.CancelSettlementCommand;
import com.symphonia.gathering.application.dto.command.ConfirmShareCommand;
import com.symphonia.gathering.application.dto.command.MarkSharesSentCommand;
import com.symphonia.gathering.application.dto.command.RecordSettlementCommand;
import com.symphonia.gathering.application.dto.command.RegisterBankAccountCommand;
import com.symphonia.gathering.application.dto.command.RevertShareCommand;
import com.symphonia.gathering.application.dto.command.UpdateSettlementCommand;
import com.symphonia.gathering.application.dto.result.PayerSharesResult;
import com.symphonia.gathering.application.dto.result.SettlementResult;
import com.symphonia.gathering.application.usecase.CancelSettlementUseCase;
import com.symphonia.gathering.application.usecase.ConfirmShareUseCase;
import com.symphonia.gathering.application.usecase.MarkSharesSentUseCase;
import com.symphonia.gathering.application.usecase.RecordSettlementUseCase;
import com.symphonia.gathering.application.usecase.RegisterBankAccountUseCase;
import com.symphonia.gathering.application.usecase.RevertShareUseCase;
import com.symphonia.gathering.application.usecase.UpdateSettlementUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.ParticipantBankAccount;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.exception.LinkTokenNotFoundException;
import com.symphonia.gathering.domain.exception.SettlementNotFoundException;
import com.symphonia.gathering.domain.policy.SettlementPolicy;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.GatheringRetentionRepository;
import com.symphonia.gathering.domain.repository.ParticipantBankAccountRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.BankAccount;
import com.symphonia.gathering.domain.vo.SplitMethod;
import com.symphonia.notification.application.dto.command.AnnounceCancellationCommand;
import com.symphonia.notification.application.dto.command.RescheduleNotificationCommand;
import com.symphonia.notification.application.dto.command.ScheduleReminderCommand;
import com.symphonia.notification.application.dto.result.NotificationScheduleResult;
import com.symphonia.notification.application.usecase.AnnounceCancellationUseCase;
import com.symphonia.notification.application.usecase.GetNotificationScheduleUseCase;
import com.symphonia.notification.application.usecase.RescheduleNotificationUseCase;
import com.symphonia.notification.application.usecase.ScheduleReminderUseCase;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

// 정산 건은 결제자가 개인 링크 토큰으로 기록하고 수정하고 취소한다. 요청한 참여자가 곧 결제자다.
// 기록과 수정은 정산 건을 바꾸기 전에 notification의 발송 예약을 먼저 잠근다.
// 발송도 예약을 잠근 뒤 정산 건을 잠그므로, 같은 순서로 잠가 교착 상태가 생기지 않는다.
// notification의 UseCase 넷은 모두 정산 건 변경이 발송에 미치는 영향이라 한 관심사로 본다.
// 같은 트랜잭션 보장을 지키려고 나누지 않는다(#159, #127).
// 분담금 상태는 참여자가 "보냈어요"로 바꾸고, 결제자가 정산 건 단위로 정정한다.
@CommandService
@RequiredArgsConstructor
public class SettlementCommandService
        implements RegisterBankAccountUseCase,
                RecordSettlementUseCase,
                UpdateSettlementUseCase,
                CancelSettlementUseCase,
                MarkSharesSentUseCase,
                ConfirmShareUseCase,
                RevertShareUseCase {
    private final ParticipantLinkTokenRepository participantLinkTokenRepository;
    private final GatheringRepository gatheringRepository;
    private final GatheringRetentionRepository gatheringRetentionRepository;
    private final ParticipantBankAccountRepository participantBankAccountRepository;
    private final SettlementRepository settlementRepository;
    private final RescheduleNotificationUseCase rescheduleNotificationUseCase;
    private final AnnounceCancellationUseCase announceCancellationUseCase;
    private final GetNotificationScheduleUseCase getNotificationScheduleUseCase;
    private final ScheduleReminderUseCase scheduleReminderUseCase;
    private final Clock clock;

    @Override
    public void register(RegisterBankAccountCommand command) {
        Long participantId = findParticipantId(command.linkToken());
        Gathering gathering = findGatheringByParticipantId(participantId);
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

    @Override
    public SettlementResult record(RecordSettlementCommand command) {
        Long participantId = findParticipantId(command.linkToken());
        Gathering gathering = findGatheringByParticipantId(participantId);
        LocalDateTime now = LocalDateTime.now(clock);
        SettlementPolicy.validateRecordable(gathering.getOpenedAt(), now);
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

        NotificationScheduleResult schedule =
                rescheduleNotificationUseCase.reschedule(
                        RescheduleNotificationCommand.of(gathering.getId(), now));

        Settlement settlement = Settlement.record(gathering.getId(), splitMethod, now);
        Settlement savedSettlement = settlementRepository.save(settlement);

        return SettlementResult.of(savedSettlement, schedule.scheduledAt());
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
        LocalDateTime now = LocalDateTime.now(clock);
        SettlementPolicy.validateRecordable(gathering.getOpenedAt(), now);
        SettlementPolicy.validateGatheringParticipants(
                gathering.hasParticipants(command.participantIds()));
        SplitMethod splitMethod =
                SplitMethod.of(
                        command.totalAmount(),
                        participantId,
                        command.participantIds(),
                        command.fixedAmounts());

        NotificationScheduleResult schedule =
                rescheduleNotificationUseCase.reschedule(
                        RescheduleNotificationCommand.of(gathering.getId(), now));

        settlement.update(splitMethod, now);
        Settlement savedSettlement = settlementRepository.save(settlement);

        return SettlementResult.of(savedSettlement, schedule.scheduledAt());
    }

    @Override
    public void cancel(CancelSettlementCommand command) {
        Long participantId = findParticipantId(command.linkToken());
        Settlement settlement = findSettlement(command.settlementId());
        SettlementPolicy.validatePayer(settlement.getPayerParticipantId(), participantId);

        LocalDateTime now = LocalDateTime.now(clock);
        settlement.cancel(now);
        settlementRepository.save(settlement);

        Optional.of(settlement)
                .filter(Settlement::isNotified)
                .ifPresent(this::announceCancellation);
    }

    // 그 결제자에게 낼 미입금 분담금을 정산 건마다 보냄으로 바꾼다. 정산 건을 잠그고 읽으므로 다른 참여자의 "보냈어요"와는 충돌하지 않는다.
    // 취소나 수정과 겹치면 잠금 없이 읽은 늦은 쪽이 버전 충돌로 실패하고, 먼저 취소된 정산 건은 잠금 조회에서 빠진다.
    // 이미 보냄이나 확인 완료인 분담금은 건너뛰어, 모두 보냈으면 그대로 돌려준다. 송금 앱을 다녀와 다시 누르는 경우를 실패로 보지 않기 위해서다.
    @Override
    public PayerSharesResult markSent(MarkSharesSentCommand command) {
        Long participantId = findParticipantId(command.linkToken());
        List<Settlement> settlements =
                settlementRepository.findAllByShareParticipantIdAndPayerForUpdate(
                        participantId, command.payerParticipantId());
        SettlementPolicy.validateSharesFound(!settlements.isEmpty());
        Gathering gathering = findGatheringByParticipantId(participantId);
        LocalDateTime now = LocalDateTime.now(clock);

        settlements.stream()
                .filter(settlement -> settlement.hasUnpaidShareOf(participantId))
                .forEach(
                        settlement -> {
                            settlement.markShareSent(participantId, now);
                            settlementRepository.save(settlement);
                        });

        return PayerSharesResult.of(
                gathering.getParticipant(command.payerParticipantId()),
                findPayerBankAccount(command.payerParticipantId(), gathering.getId()),
                settlements,
                participantId);
    }

    @Override
    public SettlementResult confirm(ConfirmShareCommand command) {
        Long participantId = findParticipantId(command.linkToken());
        Settlement settlement = findSettlement(command.settlementId());
        SettlementPolicy.validatePayer(settlement.getPayerParticipantId(), participantId);

        settlement.confirmShare(command.participantId(), LocalDateTime.now(clock));
        Settlement savedSettlement = settlementRepository.save(settlement);

        return SettlementResult.of(
                savedSettlement, findScheduledAt(savedSettlement.getGatheringId()));
    }

    // 정산 메시지를 보낸 정산 건이면 되돌린 참여자에게 24시간 뒤 리마인드를 남긴다.
    // 리마인드는 발송 예약을 잠그고 만들므로, 기록과 같은 순서가 되게 정산 건을 저장하기 전에 부른다.
    @Override
    public SettlementResult revert(RevertShareCommand command) {
        Long participantId = findParticipantId(command.linkToken());
        Settlement settlement = findSettlement(command.settlementId());
        SettlementPolicy.validatePayer(settlement.getPayerParticipantId(), participantId);

        settlement.revertShareToUnpaid(command.participantId(), LocalDateTime.now(clock));
        Optional.of(settlement)
                .filter(Settlement::isNotified)
                .ifPresent(notified -> scheduleReminder(notified, command.participantId()));
        Settlement savedSettlement = settlementRepository.save(settlement);

        return SettlementResult.of(
                savedSettlement, findScheduledAt(savedSettlement.getGatheringId()));
    }

    // 파기 시각이 지난 모임의 링크는 물리 삭제 전이라도 찾지 못한 것으로 본다. 삭제 뒤의 응답과 같게 하기 위해서다.
    // 데이터 키를 지운 모임을 복호화하지 않도록 모임을 읽기 전에 확인한다.
    private Long findParticipantId(String linkToken) {
        LocalDateTime now = LocalDateTime.now(clock);
        return participantLinkTokenRepository
                .findParticipantIdByValue(linkToken)
                .filter(
                        participantId ->
                                gatheringRetentionRepository
                                        .findByParticipantId(participantId)
                                        .filter(retention -> !retention.isExpired(now))
                                        .isPresent())
                .orElseThrow(LinkTokenNotFoundException::new);
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

    private void announceCancellation(Settlement settlement) {
        announceCancellationUseCase.announceCancellation(
                AnnounceCancellationCommand.of(settlement.getGatheringId(), settlement.getId()));
    }

    // 결제자는 계좌를 등록해야 정산 건을 기록할 수 있고 계좌는 모임을 파기할 때만 지우므로, 없으면 서버 버그라서 500으로 둔다.
    private BankAccount findPayerBankAccount(Long payerParticipantId, Long gatheringId) {
        return participantBankAccountRepository
                .findAllByParticipantIds(List.of(payerParticipantId), gatheringId)
                .stream()
                .findFirst()
                .orElseThrow()
                .getBankAccount();
    }

    private LocalDateTime findScheduledAt(Long gatheringId) {
        return getNotificationScheduleUseCase.getSchedule(gatheringId).scheduledAt();
    }

    private void scheduleReminder(Settlement settlement, Long participantId) {
        scheduleReminderUseCase.scheduleReminder(
                ScheduleReminderCommand.of(
                        settlement.getGatheringId(), participantId, settlement.getId()));
    }
}
