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
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;

// 분담금 상태는 참여자가 "보냈어요"로 바꾸고, 결제자가 정산 건 단위로 정정한다. 둘 다 개인 링크 토큰으로 요청한다.
@CommandService
@RequiredArgsConstructor
public class ShareCommandService
        implements MarkSharesSentUseCase, ConfirmShareUseCase, RevertShareUseCase {
    private final GetLinkedParticipantUseCase getLinkedParticipantUseCase;
    private final GatheringRepository gatheringRepository;
    private final ParticipantBankAccountRepository participantBankAccountRepository;
    private final SettlementRepository settlementRepository;
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

        return toResult(settlementRepository.save(settlement), participantId);
    }

    @Override
    public SettlementResult revert(RevertShareCommand command) {
        Long participantId = findParticipantId(command.linkToken());
        Settlement settlement = findSettlement(command.settlementId());
        SettlementPolicy.validatePayer(settlement.getPayerParticipantId(), participantId);

        settlement.revertShareToUnpaid(command.participantId(), LocalDateTime.now(clock));

        return toResult(settlementRepository.save(settlement), participantId);
    }

    // 응답에 정산 건이 속한 차수의 번호를 담으려면 모임의 차수가 필요하다.
    private SettlementResult toResult(Settlement settlement, Long participantId) {
        Gathering gathering =
                gatheringRepository
                        .findByParticipantId(participantId)
                        .orElseThrow(GatheringNotFoundException::new);

        return SettlementResult.of(settlement, gathering.getRound(settlement.getRoundId()));
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
}
