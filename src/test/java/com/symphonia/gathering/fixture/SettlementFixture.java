package com.symphonia.gathering.fixture;

import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.entity.SettlementShare;
import com.symphonia.gathering.domain.vo.PaymentStatus;
import com.symphonia.gathering.domain.vo.SplitMethod;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 금액을 지정한 참여자 없이 결제자를 포함한 인원이 똑같이 나누는 정산 건이다.
@Getter
@RequiredArgsConstructor
public enum SettlementFixture {
    DINNER(10_000L, LocalDateTime.of(2026, 10, 27, 20, 0)),
    SECOND_ROUND(20_000L, LocalDateTime.of(2026, 10, 27, 23, 0));

    private final long totalAmount;
    private final LocalDateTime recordedAt;

    public SplitMethod createSplitMethod(Long payerParticipantId, List<Long> participantIds) {
        return SplitMethod.of(totalAmount, payerParticipantId, participantIds, Map.of());
    }

    // 저장 전의 새 정산 건이다.
    public Settlement create(Long gatheringId, Long payerParticipantId, List<Long> participantIds) {
        return Settlement.record(
                gatheringId, createSplitMethod(payerParticipantId, participantIds), recordedAt);
    }

    public Settlement createWithId(
            Long id, Long gatheringId, Long payerParticipantId, List<Long> participantIds) {
        return builder(id, gatheringId, payerParticipantId, participantIds).build();
    }

    public Settlement createNotified(
            Long id, Long gatheringId, Long payerParticipantId, List<Long> participantIds) {
        return builder(id, gatheringId, payerParticipantId, participantIds)
                .notifiedAt(recordedAt.plusHours(1))
                .build();
    }

    public Settlement createCancelled(
            Long id, Long gatheringId, Long payerParticipantId, List<Long> participantIds) {
        return builder(id, gatheringId, payerParticipantId, participantIds)
                .cancelledAt(recordedAt.plusHours(1))
                .build();
    }

    // 첫 분담만 입금 상태를 바꾸고 나머지 분담은 미입금으로 둔다.
    public Settlement createWithFirstShareStatus(
            Long id,
            Long gatheringId,
            Long payerParticipantId,
            List<Long> participantIds,
            PaymentStatus paymentStatus) {
        List<SettlementShare> shares =
                createSplitMethod(payerParticipantId, participantIds).shares();
        SettlementShare firstShare = shares.getFirst();
        SettlementShare changedFirstShare =
                SettlementShare.builder()
                        .participantId(firstShare.getParticipantId())
                        .amount(firstShare.getAmount())
                        .fixed(firstShare.isFixed())
                        .paymentStatus(paymentStatus)
                        .build();
        List<SettlementShare> changedShares =
                Stream.concat(Stream.of(changedFirstShare), shares.stream().skip(1)).toList();

        return builder(id, gatheringId, payerParticipantId, participantIds)
                .shares(changedShares)
                .build();
    }

    private Settlement.SettlementBuilder builder(
            Long id, Long gatheringId, Long payerParticipantId, List<Long> participantIds) {
        SplitMethod splitMethod = createSplitMethod(payerParticipantId, participantIds);

        return Settlement.builder()
                .id(id)
                .gatheringId(gatheringId)
                .payerParticipantId(payerParticipantId)
                .totalAmount(totalAmount)
                .shares(splitMethod.shares())
                .recordedAt(recordedAt)
                .version(0L);
    }
}
