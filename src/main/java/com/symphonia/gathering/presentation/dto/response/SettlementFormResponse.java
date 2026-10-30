package com.symphonia.gathering.presentation.dto.response;

import com.symphonia.gathering.application.dto.result.SettlementFormResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record SettlementFormResponse(
        @Schema(description = "정산 건을 기록할 수 있는지 여부") boolean recordable,
        @Schema(description = "요청한 참여자의 계좌 등록 여부") boolean bankAccountRegistered,
        @Schema(description = "계좌를 등록할 때 함께 보낼 동의 안내 버전") String bankAccountConsentVersion,
        @Schema(description = "모임 참여자") List<SettlementParticipantResponse> participants,
        @Schema(description = "인원 기본값 (직전 정산 건의 인원, 첫 건이면 참여자 전원)")
                List<Long> defaultParticipantIds,
        @Schema(description = "요청한 참여자가 결제자로 기록한, 취소하지 않은 정산 건")
                List<SettlementResponse> settlements) {
    public static SettlementFormResponse from(SettlementFormResult result) {
        return new SettlementFormResponse(
                result.recordable(),
                result.bankAccountRegistered(),
                result.bankAccountConsentVersion(),
                result.participants().stream().map(SettlementParticipantResponse::from).toList(),
                result.defaultParticipantIds(),
                result.settlements().stream().map(SettlementResponse::from).toList());
    }
}
