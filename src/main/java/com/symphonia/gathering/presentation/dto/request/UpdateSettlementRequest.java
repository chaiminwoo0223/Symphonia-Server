package com.symphonia.gathering.presentation.dto.request;

import com.symphonia.gathering.application.dto.command.UpdateSettlementCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record UpdateSettlementRequest(
        @Positive @Max(100_000_000) @Schema(description = "카드 결제 총액 (원)") long totalAmount,
        @NotEmpty @Schema(description = "결제자를 포함한 정산 건 인원의 참여자 ID")
                List<@NotNull Long> participantIds,
        @NotNull @Schema(description = "결제자가 금액을 지정한 참여자와 그 금액. 없으면 빈 목록")
                List<@Valid @NotNull FixedAmountRequest> fixedAmounts) {
    @AssertTrue(message = "같은 참여자의 금액을 두 번 지정할 수 없습니다.")
    @Schema(hidden = true)
    public boolean isFixedAmountParticipantsDistinct() {
        return fixedAmounts == null
                || fixedAmounts.stream().map(FixedAmountRequest::participantId).distinct().count()
                        == fixedAmounts.size();
    }

    public UpdateSettlementCommand toCommand(String linkToken, Long settlementId) {
        return new UpdateSettlementCommand(
                linkToken, settlementId, totalAmount, participantIds, toFixedAmountMap());
    }

    private Map<Long, Long> toFixedAmountMap() {
        return fixedAmounts.stream()
                .collect(
                        Collectors.toMap(
                                FixedAmountRequest::participantId, FixedAmountRequest::amount));
    }
}
