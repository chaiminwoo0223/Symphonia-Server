package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.gathering.application.dto.command.ConfirmShareCommand;
import com.symphonia.gathering.application.dto.command.MarkSharesSentCommand;
import com.symphonia.gathering.application.dto.command.RevertShareCommand;
import com.symphonia.gathering.application.dto.result.PayerSharesResult;
import com.symphonia.gathering.application.dto.result.SettlementResult;
import com.symphonia.gathering.application.usecase.ConfirmShareUseCase;
import com.symphonia.gathering.application.usecase.GetLinkedParticipantUseCase;
import com.symphonia.gathering.application.usecase.MarkSharesSentUseCase;
import com.symphonia.gathering.application.usecase.RevertShareUseCase;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.exception.SettlementNotFoundException;
import com.symphonia.gathering.domain.policy.SettlementPolicy;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.ParticipantBankAccountRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.gathering.domain.vo.BankAccount;
import com.symphonia.notification.application.dto.command.ScheduleReminderCommand;
import com.symphonia.notification.application.usecase.GetNotificationScheduleUseCase;
import com.symphonia.notification.application.usecase.ScheduleReminderUseCase;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

// 분담금 상태는 참여자가 "보냈어요"로 바꾸고, 결제자가 정산 건 단위로 정정한다. 둘 다 개인 링크 토큰으로 요청한다.
// notification의 UseCase 둘은 응답에 담을 발송 예정 시각과 되돌린 분담금의 리마인드라서, 분담금 상태 변경이 발송에 미치는 영향이라는 한 관심사다.
@CommandService
@RequiredArgsConstructor
public class ShareCommandService
        implements MarkSharesSentUseCase, ConfirmShareUseCase, RevertShareUseCase {
    private final GetLinkedParticipantUseCase getLinkedParticipantUseCase;
    private final GatheringRepository gatheringRepository;
    private final ParticipantBankAccountRepository participantBankAccountRepository;
    private final SettlementRepository settlementRepository;
    private final GetNotificationScheduleUseCase getNotificationScheduleUseCase;
    private final ScheduleReminderUseCase scheduleReminderUseCase;
    private final Clock clock;

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
        Gathering gathering =
                gatheringRepository
                        .findByParticipantId(participantId)
                        .orElseThrow(GatheringNotFoundException::new);
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

    private Long findParticipantId(String linkToken) {
        return getLinkedParticipantUseCase.getLinkedParticipant(linkToken).participantId();
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

    private Settlement findSettlement(Long settlementId) {
        return settlementRepository
                .findById(settlementId)
                .orElseThrow(SettlementNotFoundException::new);
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
