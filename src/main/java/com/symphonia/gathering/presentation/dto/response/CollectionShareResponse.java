package com.symphonia.gathering.presentation.dto.response;

import com.symphonia.gathering.application.dto.result.CollectionShareResult;
import com.symphonia.gathering.domain.vo.ShareStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record CollectionShareResponse(
        @Schema(description = "정산 건 ID. 확인 완료와 미입금 되돌리기 경로에 쓴다") Long settlementId,
        @Schema(description = "정산 건이 속한 차수의 ID") Long roundId,
        @Schema(description = "정산 건이 속한 차수의 번호") int roundNumber,
        @Schema(description = "그 정산 건에서 받을 금액 (원)") long amount,
        @Schema(description = "분담금 상태 (UNPAID: 미입금, SENT: 보냄, CONFIRMED: 확인 완료)")
                ShareStatus status) {
    public static CollectionShareResponse from(CollectionShareResult result) {
        return new CollectionShareResponse(
                result.settlementId(),
                result.roundId(),
                result.roundNumber(),
                result.amount(),
                result.status());
    }
}
