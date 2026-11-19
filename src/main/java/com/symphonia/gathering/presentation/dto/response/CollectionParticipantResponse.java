package com.symphonia.gathering.presentation.dto.response;

import com.symphonia.gathering.application.dto.result.CollectionParticipantResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record CollectionParticipantResponse(
        @Schema(description = "참여자 ID") Long participantId,
        @Schema(description = "참여자 이름") String name,
        @Schema(description = "참여자 전체 휴대폰 번호 (하이픈 없는 숫자). 카카오톡 방 초대에만 쓴다") String phoneNumber,
        @Schema(description = "이 참여자에게서 받을 금액의 합 (원)") long totalAmount,
        @Schema(description = "정산 건별 받을 금액") List<CollectionShareResponse> shares) {
    public static CollectionParticipantResponse from(CollectionParticipantResult result) {
        return new CollectionParticipantResponse(
                result.participantId(),
                result.name(),
                result.phoneNumber(),
                result.totalAmount(),
                result.shares().stream().map(CollectionShareResponse::from).toList());
    }

    // 로그에 전체 번호가 남지 않게 번호를 뺀다.
    @Override
    public String toString() {
        return "CollectionParticipantResponse[participantId=" + participantId + "]";
    }
}
