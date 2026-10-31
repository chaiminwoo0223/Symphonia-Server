package com.symphonia.gathering.presentation.dto.response;

import com.symphonia.gathering.application.dto.result.PayerSharesResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record PayerSharesResponse(
        @Schema(description = "결제자 참여자 ID") Long payerParticipantId,
        @Schema(description = "결제자 이름") String payerName,
        @Schema(description = "결제자 은행 이름") String bankName,
        @Schema(description = "결제자 계좌번호") String accountNumber,
        @Schema(description = "아직 보내지 않은 분담액의 합 (원)") long unpaidAmount,
        @Schema(description = "아직 보내지 않은 분담금이 있는지 여부") boolean hasUnpaidShares,
        @Schema(description = "정산 건별 본인 분담금과 계산 근거") List<ShareBreakdownResponse> shares) {
    public static PayerSharesResponse from(PayerSharesResult result) {
        return new PayerSharesResponse(
                result.payerParticipantId(),
                result.payerName(),
                result.bankName(),
                result.accountNumber(),
                result.unpaidAmount(),
                result.hasUnpaidShares(),
                result.shares().stream().map(ShareBreakdownResponse::from).toList());
    }
}
