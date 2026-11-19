package com.symphonia.gathering.presentation.dto.response;

import com.symphonia.gathering.application.dto.result.SettlementFormResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record SettlementFormResponse(
        @Schema(description = "정산 건을 기록할 수 있는지 여부") boolean recordable,
        @Schema(description = "정산 건이 없는 빈 차수를 포함한 모임의 차수") List<GatheringRoundResponse> rounds,
        @Schema(description = "모임 참여자") List<SettlementParticipantResponse> participants,
        @Schema(description = "인원 기본값 (직전 정산 건의 인원, 첫 건이면 참여자 전원)")
                List<Long> defaultParticipantIds,
        @Schema(description = "요청한 참여자가 결제자로 기록한, 취소하지 않은 정산 건")
                List<SettlementResponse> settlements,
        @Schema(description = "settlements의 카드 결제 총액 합계 (원, 취소하지 않은 정산 건만)") long totalAmountSum,
        @Schema(description = "settlements의 받을 금액 합계 (원, 취소하지 않은 정산 건만)") long receivableAmountSum,
        @Schema(description = "모임의 개인정보를 파기할 예정 시각 (모든 분담금이 끝나면 앞당겨짐)") LocalDateTime deletesAt) {
    public static SettlementFormResponse from(SettlementFormResult result) {
        return new SettlementFormResponse(
                result.recordable(),
                result.rounds().stream().map(GatheringRoundResponse::from).toList(),
                result.participants().stream().map(SettlementParticipantResponse::from).toList(),
                result.defaultParticipantIds(),
                result.settlements().stream().map(SettlementResponse::from).toList(),
                result.totalAmountSum(),
                result.receivableAmountSum(),
                result.deletesAt());
    }
}
