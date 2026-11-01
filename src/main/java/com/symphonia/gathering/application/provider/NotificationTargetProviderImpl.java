package com.symphonia.gathering.application.provider;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.entity.SettlementShare;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.notification.application.dto.command.NotifySettlementsCommand;
import com.symphonia.notification.application.dto.result.NotificationTargetResult;
import com.symphonia.notification.application.provider.NotificationTargetProvider;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

// notification이 정산 메시지 발송을 시작할 때 부른다. 부른 쪽의 트랜잭션에 합류해, 발송 시각과 발송 기록이 함께 커밋된다.
// 부른 쪽이 예약을 먼저 잠그고 이 구현이 정산 건을 잠근다. 기록과 수정도 예약을 먼저 잠그므로 같은 순서로 잠가 교착 상태가 생기지 않는다.
@CommandService
@RequiredArgsConstructor
public class NotificationTargetProviderImpl implements NotificationTargetProvider {
    private final SettlementRepository settlementRepository;

    // 예약 뒤에 모두 취소됐으면 보낼 정산 건이 없어 빈 목록을 돌려준다.
    @Override
    public List<NotificationTargetResult> notifySettlements(NotifySettlementsCommand command) {
        List<Settlement> settlements =
                settlementRepository.findAllUnnotifiedByGatheringIdForUpdate(command.gatheringId());
        settlements.forEach(
                settlement -> {
                    settlement.markNotified(command.notifiedAt());
                    settlementRepository.save(settlement);
                });

        return groupUnpaidSettlementIdsByParticipantId(settlements).entrySet().stream()
                .map(entry -> NotificationTargetResult.of(entry.getKey(), entry.getValue()))
                .toList();
    }

    // 수신자마다 아직 내지 않은 분담금이 있는 정산 건을 묶어 한 메시지로 보낸다. 0원 분담금은 미입금이 아니라서 빠진다.
    private Map<Long, List<Long>> groupUnpaidSettlementIdsByParticipantId(
            List<Settlement> settlements) {
        Map<Long, List<Long>> settlementIdsByParticipantId = new LinkedHashMap<>();
        settlements.forEach(
                settlement ->
                        settlement.getShares().stream()
                                .filter(SettlementShare::isUnpaid)
                                .forEach(
                                        share ->
                                                settlementIdsByParticipantId
                                                        .computeIfAbsent(
                                                                share.getParticipantId(),
                                                                _ -> new ArrayList<>())
                                                        .add(settlement.getId())));
        return settlementIdsByParticipantId;
    }
}
