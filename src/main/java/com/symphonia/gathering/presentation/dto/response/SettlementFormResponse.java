package com.symphonia.gathering.presentation.dto.response;

import com.symphonia.gathering.application.dto.result.SettlementFormResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record SettlementFormResponse(
        @Schema(description = "정산 건을 기록할 수 있는지 여부") boolean recordable,
        @Schema(description = "기록 기간이 끝나는 시각 (연장했으면 연장한 시각)") LocalDateTime recordingClosesAt,
        @Schema(description = "기록 기간을 이미 연장했는지 여부 (연장은 한 번만 할 수 있음)") boolean recordingExtended,
        @Schema(description = "모임장이 직접 참여할 때 보낼 동의 안내문 버전 (참여 화면 조회의 consentVersion과 같음)")
                String consentVersion,
        @Schema(description = "정산 건이 없는 빈 차수를 포함한 모임의 차수") List<GatheringRoundResponse> rounds,
        @Schema(description = "결제자와 인원을 고를 모임 참여자 (휴대폰은 마스킹)")
                List<SettlementParticipantResponse> participants,
        @Schema(description = "인원 기본값 (직전 정산 건의 인원, 첫 건이면 참여자 전원)")
                List<Long> defaultParticipantIds,
        @Schema(description = "모임의 취소하지 않은 정산 건") List<SettlementResponse> settlements,
        @Schema(description = "settlements의 카드 결제 총액 합계 (원, 취소하지 않은 정산 건만)") long totalAmountSum,
        @Schema(description = "settlements의 받을 금액 합계 (원, 취소하지 않은 정산 건만)") long receivableAmountSum,
        @Schema(description = "모임의 개인정보를 파기할 예정 시각 (모든 분담금이 끝나면 앞당겨짐)") LocalDateTime deletesAt) {
    public static SettlementFormResponse from(SettlementFormResult result) {
        return new SettlementFormResponse(
                result.recordable(),
                result.recordingClosesAt(),
                result.recordingExtended(),
                result.consentVersion(),
                result.rounds().stream().map(GatheringRoundResponse::from).toList(),
                result.participants().stream().map(SettlementParticipantResponse::from).toList(),
                result.defaultParticipantIds(),
                result.settlements().stream().map(SettlementResponse::from).toList(),
                result.totalAmountSum(),
                result.receivableAmountSum(),
                result.deletesAt());
    }
}
