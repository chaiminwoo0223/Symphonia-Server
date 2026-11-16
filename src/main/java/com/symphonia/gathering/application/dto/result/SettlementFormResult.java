package com.symphonia.gathering.application.dto.result;

import java.time.LocalDateTime;
import java.util.List;

// 결제자가 정산 건을 기록하는 화면에 필요한 값이다.
// rounds는 정산 건이 없는 빈 차수를 포함한 모임의 차수이고, 정산 건을 기록할 차수를 고르는 데 쓴다.
// defaultParticipantIds는 직전 정산 건의 인원이고, 첫 건이면 참여자 전원이다.
// settlements는 요청한 참여자가 결제자로 기록한, 취소하지 않은 정산 건이다. 고치거나 취소할 정산 건을 고르는 데 쓴다.
// totalAmountSum과 receivableAmountSum은 settlements의 카드 결제 총액과 받을 금액을 각각 더한 값이다.
// 다른 결제자의 금액은 포함하지 않는다.
// deletesAt은 모임의 개인정보를 파기할 시각이다. 모든 분담금이 끝나면 앞당겨지고, 미입금으로 되돌리면 다시 늦춰진다.
public record SettlementFormResult(
        boolean recordable,
        boolean bankAccountRegistered,
        String bankAccountConsentVersion,
        List<GatheringRoundResult> rounds,
        List<SettlementParticipantResult> participants,
        List<Long> defaultParticipantIds,
        List<SettlementResult> settlements,
        long totalAmountSum,
        long receivableAmountSum,
        LocalDateTime deletesAt) {
    public static SettlementFormResult of(
            boolean recordable,
            boolean bankAccountRegistered,
            String bankAccountConsentVersion,
            List<GatheringRoundResult> rounds,
            List<SettlementParticipantResult> participants,
            List<Long> defaultParticipantIds,
            List<SettlementResult> settlements,
            LocalDateTime deletesAt) {
        return new SettlementFormResult(
                recordable,
                bankAccountRegistered,
                bankAccountConsentVersion,
                rounds,
                participants,
                defaultParticipantIds,
                settlements,
                settlements.stream().mapToLong(SettlementResult::totalAmount).sum(),
                settlements.stream().mapToLong(SettlementResult::receivableAmount).sum(),
                deletesAt);
    }
}
