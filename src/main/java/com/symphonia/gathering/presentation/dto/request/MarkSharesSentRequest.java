package com.symphonia.gathering.presentation.dto.request;

import com.symphonia.gathering.application.dto.command.MarkSharesSentCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public record MarkSharesSentRequest(
        @NotNull @Schema(description = "분담금을 보낸 결제자의 참여자 ID") Long payerParticipantId,
        @NotEmpty
                @Schema(
                        description =
                                "화면에서 본 그 결제자의 분담금. 분담금 조회 응답의 settlementId와 amount를 담는다. 미입금 분담금이 빠졌거나 정산 건이나 금액이 지금과 다르면 409(UNPAID_SHARE_CONFLICT)")
                List<@Valid @NotNull SeenShare> shares) {
    @AssertTrue(message = "같은 정산 건을 두 번 담을 수 없습니다.")
    @Schema(hidden = true)
    public boolean isSettlementsDistinct() {
        if (shares == null) {
            return true;
        }
        List<Long> settlementIds =
                shares.stream().filter(Objects::nonNull).map(SeenShare::settlementId).toList();

        return settlementIds.stream().distinct().count() == settlementIds.size();
    }

    public MarkSharesSentCommand toCommand(String linkToken) {
        return new MarkSharesSentCommand(linkToken, payerParticipantId, toSeenAmountMap());
    }

    public record SeenShare(
            @NotNull @Schema(description = "분담금이 속한 정산 건 ID") Long settlementId,
            @NotNull @PositiveOrZero @Schema(description = "화면에서 본 분담금 (원)") Long amount) {}

    private Map<Long, Long> toSeenAmountMap() {
        return shares.stream()
                .collect(Collectors.toMap(SeenShare::settlementId, SeenShare::amount));
    }
}
