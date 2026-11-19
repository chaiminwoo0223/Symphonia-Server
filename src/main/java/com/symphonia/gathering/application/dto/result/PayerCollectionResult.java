package com.symphonia.gathering.application.dto.result;

import com.symphonia.gathering.domain.entity.Gathering;
import com.symphonia.gathering.domain.entity.Settlement;
import com.symphonia.gathering.domain.entity.SettlementShare;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 결제자가 개인 링크에서 보는 수금 화면이다. 결제자로 지정된 취소하지 않은 정산 건을 참여자별로 묶는다.
// 참여자는 처음 분담금이 나온 정산 건의 순서를 따른다. totalAmount는 모든 참여자의 totalAmount를 더한 값이다.
// deletesAt은 모임의 개인정보를 파기할 시각이다. 번호와 금액을 언제까지 볼 수 있는지 안내하는 데 쓴다.
public record PayerCollectionResult(
        LocalDateTime deletesAt, long totalAmount, List<CollectionParticipantResult> participants) {
    public static PayerCollectionResult of(
            LocalDateTime deletesAt, Gathering gathering, List<Settlement> payerSettlements) {
        Map<Long, List<CollectionShareResult>> sharesByParticipantId = new LinkedHashMap<>();
        for (Settlement settlement : payerSettlements) {
            for (SettlementShare share : settlement.getShares()) {
                sharesByParticipantId
                        .computeIfAbsent(share.getParticipantId(), _ -> new ArrayList<>())
                        .add(
                                CollectionShareResult.of(
                                        settlement,
                                        share,
                                        gathering.getRound(settlement.getRoundId())));
            }
        }
        List<CollectionParticipantResult> participants =
                sharesByParticipantId.entrySet().stream()
                        .map(
                                entry ->
                                        CollectionParticipantResult.of(
                                                gathering.getParticipant(entry.getKey()),
                                                entry.getValue()))
                        .toList();

        return new PayerCollectionResult(
                deletesAt,
                participants.stream().mapToLong(CollectionParticipantResult::totalAmount).sum(),
                participants);
    }
}
