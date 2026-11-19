package com.symphonia.gathering.presentation.dto.request;

import com.symphonia.gathering.application.dto.command.RecordSettlementCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public record RecordSettlementRequest(
        @NotNull
                @Schema(
                        description =
                                "기록 요청마다 클라이언트가 만드는 UUID. 같은 값으로 다시 보내면 정산 건을 새로 만들지 않고 처음 기록한 정산 건을 돌려준다")
                UUID clientRequestId,
        @NotNull @Schema(description = "결제자로 지정한 참여자 ID") Long payerParticipantId,
        @NotNull @Schema(description = "정산 건이 속한 차수의 ID") Long roundId,
        @Positive @Max(100_000_000) @Schema(description = "카드 결제 총액 (원)") long totalAmount,
        @NotEmpty @Schema(description = "결제자를 포함한 정산 건 인원의 참여자 ID")
                List<@NotNull Long> participantIds,
        @NotNull @Schema(description = "모임장이 금액을 지정한 참여자와 그 금액. 없으면 빈 목록")
                List<@Valid @NotNull FixedAmountRequest> fixedAmounts) {
    @AssertTrue(message = "같은 참여자의 금액을 두 번 지정할 수 없습니다.")
    @Schema(hidden = true)
    public boolean isFixedAmountParticipantsDistinct() {
        return fixedAmounts == null
                || fixedAmounts.stream().map(FixedAmountRequest::participantId).distinct().count()
                        == fixedAmounts.size();
    }

    public RecordSettlementCommand toCommand(Long memberId, Long gatheringId) {
        return new RecordSettlementCommand(
                memberId,
                gatheringId,
                payerParticipantId,
                clientRequestId,
                roundId,
                totalAmount,
                participantIds,
                toFixedAmountMap());
    }

    private Map<Long, Long> toFixedAmountMap() {
        return fixedAmounts.stream()
                .collect(
                        Collectors.toMap(
                                FixedAmountRequest::participantId, FixedAmountRequest::amount));
    }
}
