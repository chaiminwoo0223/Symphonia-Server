package com.symphonia.gathering.presentation.dto.response;

import com.symphonia.gathering.application.dto.result.PayerCollectionResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record PayerCollectionResponse(
        @Schema(description = "모임의 개인정보를 파기할 예정 시각 (모든 분담금이 끝나면 앞당겨짐)") LocalDateTime deletesAt,
        @Schema(description = "모든 참여자에게서 받을 금액의 합 (원)") long totalAmount,
        @Schema(description = "받을 금액이 있는 참여자. 결제자로 지정된 취소하지 않은 정산 건 기준")
                List<CollectionParticipantResponse> participants) {
    public static PayerCollectionResponse from(PayerCollectionResult result) {
        return new PayerCollectionResponse(
                result.deletesAt(),
                result.totalAmount(),
                result.participants().stream().map(CollectionParticipantResponse::from).toList());
    }
}
