package com.symphonia.gathering.application.service;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.gathering.application.dto.command.ConfirmShareCommand;
import com.symphonia.gathering.application.dto.command.MarkSharesSentCommand;
import com.symphonia.gathering.application.dto.command.RevertShareCommand;
import com.symphonia.gathering.application.dto.result.PayerSharesResult;
import com.symphonia.gathering.application.dto.result.SettlementResult;
import com.symphonia.gathering.application.event.SharesSentEvent;
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
import com.symphonia.gathering.domain.repository.SettlementRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

// 분담금 상태는 참여자가 "보냈어요"로 바꾸고, 결제자가 정산 건 단위로 정정한다. 둘 다 개인 링크 토큰으로 요청한다.
@CommandService
@RequiredArgsConstructor
public class ShareCommandService
        implements MarkSharesSentUseCase, ConfirmShareUseCase, RevertShareUseCase {
    private final GetLinkedParticipantUseCase getLinkedParticipantUseCase;
    private final GatheringRepository gatheringRepository;
    private final SettlementRepository settlementRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    // 그 결제자에게 낼 미입금 분담금을 정산 건마다 보냄으로 바꾼다.
    // 모임 행을 먼저 잠가 정산 건 기록, 수정, 취소와 차례로 처리한다. 정산 건만 잠그면 잠금을 기다리는 사이 기록된 정산 건이 대조에서 빠진다.
    // 먼저 끝난 기록, 수정, 취소는 잠근 뒤 읽은 분담금에 반영돼, 요청과 어긋나면 거절한다.
    // 확인 완료와 되돌리기는 모임을 잠그지 않으므로 정산 건도 잠그고 읽는다.
    // 이미 보냄이나 확인 완료인 분담금은 건너뛰어, 모두 보냈으면 그대로 돌려준다. 송금 앱을 다녀와 다시 누르는 경우를 실패로 보지 않기 위해서다.
    // 차수의 참여자 이름에 쓰는 정산 건은 잠그지 않고 읽는다. 이름만 보여 주고 상태는 바꾸지 않아 다른 정산 건의 변경과 겹쳐도 충돌하지 않는다.
    @Override
    public PayerSharesResult markSent(MarkSharesSentCommand command) {
        Long participantId = findParticipantId(command.linkToken());
        Gathering gathering =
                gatheringRepository
                        .findByParticipantIdForUpdate(participantId)
                        .orElseThrow(GatheringNotFoundException::new);
        List<Settlement> settlements =
                settlementRepository.findAllByShareParticipantIdAndPayerForUpdate(
                        participantId, command.payerParticipantId());
        SettlementPolicy.validateSeenShares(settlements, participantId, command.seenAmounts());

        List<Settlement> gatheringSettlements =
                settlementRepository.findAllByGatheringId(gathering.getId());
        LocalDateTime now = LocalDateTime.now(clock);

        List<Settlement> unpaidSettlements =
                settlements.stream()
                        .filter(settlement -> settlement.hasUnpaidShareOf(participantId))
                        .toList();
        unpaidSettlements.forEach(
                settlement -> {
                    settlement.markShareSent(participantId, now);
                    settlementRepository.save(settlement);
                });

        unpaidSettlements.stream()
                .findAny()
                .ifPresent(_ -> eventPublisher.publishEvent(new SharesSentEvent()));

        return PayerSharesResult.of(
                gathering.getParticipant(command.payerParticipantId()),
                settlements,
                participantId,
                gathering,
                gatheringSettlements);
    }

    @Override
    public SettlementResult confirm(ConfirmShareCommand command) {
        Long participantId = findParticipantId(command.linkToken());
        Settlement settlement = findSettlement(command.settlementId());
        SettlementPolicy.validatePayer(settlement.getPayerParticipantId(), participantId);

        boolean changed =
                settlement.confirmShare(command.participantId(), LocalDateTime.now(clock));

        return toResult(changed ? settlementRepository.save(settlement) : settlement);
    }

    @Override
    public SettlementResult revert(RevertShareCommand command) {
        Long participantId = findParticipantId(command.linkToken());
        Settlement settlement = findSettlement(command.settlementId());
        SettlementPolicy.validatePayer(settlement.getPayerParticipantId(), participantId);

        boolean changed =
                settlement.revertShareToUnpaid(command.participantId(), LocalDateTime.now(clock));

        return toResult(changed ? settlementRepository.save(settlement) : settlement);
    }

    private Long findParticipantId(String linkToken) {
        return getLinkedParticipantUseCase.getLinkedParticipant(linkToken).participantId();
    }

    private Settlement findSettlement(Long settlementId) {
        return settlementRepository
                .findById(settlementId)
                .orElseThrow(SettlementNotFoundException::new);
    }

    // 응답에 정산 건이 속한 차수의 번호를 담으려면 모임의 차수가 필요하다.
    private SettlementResult toResult(Settlement settlement) {
        Gathering gathering =
                gatheringRepository
                        .findById(settlement.getGatheringId())
                        .orElseThrow(GatheringNotFoundException::new);

        return SettlementResult.of(settlement, gathering.getRound(settlement.getRoundId()));
    }
}
