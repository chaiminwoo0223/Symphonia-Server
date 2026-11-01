package com.symphonia.gathering.application.provider;

import com.symphonia.common.annotation.CommandService;
import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Participant;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.exception.GatheringNotFoundException;
import com.symphonia.gathering.domain.provider.GatheringTokenProvider;
import com.symphonia.gathering.domain.repository.GatheringRepository;
import com.symphonia.gathering.domain.repository.ParticipantLinkTokenRepository;
import com.symphonia.gathering.domain.repository.SettlementRepository;
import com.symphonia.notification.application.dto.command.ComposeNotificationCommand;
import com.symphonia.notification.application.dto.result.NotificationContentResult;
import com.symphonia.notification.application.dto.result.NotificationContentResult.PayerAmountResult;
import com.symphonia.notification.application.provider.NotificationContentProvider;
import com.symphonia.notification.domain.vo.NotificationType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

// notification이 발송 기록을 맡고 보내기 직전에 부른다. 부른 쪽의 트랜잭션에 합류해, 새 링크 토큰의 해시가 발송 기록과 함께 커밋된다.
// 보내기 직전의 분담금 상태로 금액을 다시 계산하므로, 그사이 보냈거나 취소된 분담금은 메시지에 담기지 않는다.
@CommandService
@RequiredArgsConstructor
public class NotificationContentProviderImpl implements NotificationContentProvider {
    private final SettlementRepository settlementRepository;
    private final GatheringRepository gatheringRepository;
    private final ParticipantLinkTokenRepository participantLinkTokenRepository;
    private final GatheringTokenProvider gatheringTokenProvider;

    // 금액을 먼저 계산해, 담을 금액이 없을 때는 참여자 정보를 복호화하는 모임 조회와 토큰 발급을 하지 않는다.
    @Override
    public Optional<NotificationContentResult> compose(ComposeNotificationCommand command) {
        Map<Long, Long> amountsByPayerId =
                amountsByPayerId(
                        settlementRepository.findAllByIds(command.settlementIds()), command);
        if (amountsByPayerId.isEmpty()) {
            return Optional.empty();
        }

        // 부른 쪽이 발송 기록을 잠갔으므로 FK 때문에 모임을 지울 수 없어, 모임은 항상 있다.
        Gathering gathering =
                gatheringRepository
                        .findById(command.gatheringId())
                        .orElseThrow(GatheringNotFoundException::new);
        Participant recipient = gathering.getParticipant(command.participantId());

        return Optional.of(
                NotificationContentResult.of(
                        recipient.getPhoneNumber().value(),
                        recipient.getName(),
                        gathering.getName(),
                        gathering.getOpenedAt().toLocalDate(),
                        payerAmountsOf(gathering, amountsByPayerId),
                        issueLinkToken(command.participantId())));
    }

    // 보낼 금액을 결제자별로 합한다. 메시지를 받은 참여자가 결제자마다 얼마를 보낼지 바로 알 수 있게 하기 위해서다.
    // 정산 메시지는 아직 내지 않은 분담금만, 취소 알림은 취소된 정산 건의 분담금만 담는다.
    private Map<Long, Long> amountsByPayerId(
            List<Settlement> settlements, ComposeNotificationCommand command) {
        Long participantId = command.participantId();

        return settlements.stream()
                .filter(settlement -> isAnnounceable(settlement, command.type(), participantId))
                .collect(
                        Collectors.groupingBy(
                                Settlement::getPayerParticipantId,
                                LinkedHashMap::new,
                                Collectors.summingLong(
                                        settlement ->
                                                settlement.getShareOf(participantId).getAmount())));
    }

    private boolean isAnnounceable(
            Settlement settlement, NotificationType type, Long participantId) {
        return switch (type) {
            case SETTLEMENT -> settlement.hasOutstandingShareOf(participantId);
            case CANCELLATION -> settlement.hasCancelledShareOf(participantId);
        };
    }

    private List<PayerAmountResult> payerAmountsOf(
            Gathering gathering, Map<Long, Long> amountsByPayerId) {
        return amountsByPayerId.entrySet().stream()
                .map(
                        entry ->
                                PayerAmountResult.of(
                                        gathering.getParticipant(entry.getKey()).getName(),
                                        entry.getValue()))
                .toList();
    }

    // 보낼 때마다 새 개인 링크 토큰을 하나 더 발급한다. 앞서 받은 메시지의 링크도 계속 쓸 수 있다.
    private String issueLinkToken(Long participantId) {
        String linkToken = gatheringTokenProvider.generate();
        participantLinkTokenRepository.save(linkToken, participantId);
        return linkToken;
    }
}
