package com.symphonia.pairing.presentation.dto.request;

import com.symphonia.pairing.application.dto.query.GetMemberPairingFeedbacksQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record GetMemberPairingFeedbacksRequest(
        @Positive @Schema(description = "이전 페이지 응답의 nextCursor. 첫 페이지는 보내지 않습니다.") Long cursor,
        @Min(1) @Max(MAX_SIZE) @Schema(description = "한 페이지에 읽을 피드백 수. 보내지 않으면 20입니다.")
                Integer size) {
    private static final int DEFAULT_SIZE = 20;
    static final int MAX_SIZE = 50;

    public GetMemberPairingFeedbacksQuery toQuery(String memberId) {
        return GetMemberPairingFeedbacksQuery.of(
                Long.parseLong(memberId), cursor, size == null ? DEFAULT_SIZE : size);
    }
}
