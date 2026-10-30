package com.symphonia.gathering.application.dto.result;

import java.util.List;

// 결제자가 정산 건을 기록하는 화면에 필요한 값이다.
// defaultParticipantIds는 직전 정산 건의 인원이고, 첫 건이면 참여자 전원이다.
// settlements는 요청한 참여자가 결제자로 기록한, 취소하지 않은 정산 건이다. 고치거나 취소할 정산 건을 고르는 데 쓴다.
public record SettlementFormResult(
        boolean recordable,
        boolean bankAccountRegistered,
        String bankAccountConsentVersion,
        List<SettlementParticipantResult> participants,
        List<Long> defaultParticipantIds,
        List<SettlementResult> settlements) {
    public static SettlementFormResult of(
            boolean recordable,
            boolean bankAccountRegistered,
            String bankAccountConsentVersion,
            List<SettlementParticipantResult> participants,
            List<Long> defaultParticipantIds,
            List<SettlementResult> settlements) {
        return new SettlementFormResult(
                recordable,
                bankAccountRegistered,
                bankAccountConsentVersion,
                participants,
                defaultParticipantIds,
                settlements);
    }
}
