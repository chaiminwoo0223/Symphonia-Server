package com.symphonia.gathering.presentation.dto.response;

import com.symphonia.gathering.application.dto.result.SharesResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record SharesResponse(
        @Schema(description = "모임의 개인정보를 파기할 예정 시각 (모든 분담금이 끝나면 앞당겨짐)") LocalDateTime deletesAt,
        @Schema(description = "결제자별 분담금 (보낼 분담금이 남은 결제자가 앞)") List<PayerSharesResponse> payers) {
    public static SharesResponse from(SharesResult result) {
        return new SharesResponse(
                result.deletesAt(),
                result.payers().stream().map(PayerSharesResponse::from).toList());
    }
}
