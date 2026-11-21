package com.symphonia.pairing.presentation.dto.response;

import com.symphonia.pairing.application.dto.result.MemberPairingFeedbacksResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record MemberPairingFeedbacksResponse(
        @Schema(description = "최근에 낸 순서의 피드백") List<MemberPairingFeedbackResponse> feedbacks,
        @Schema(description = "다음 페이지를 읽을 때 cursor로 보내는 값. 마지막 페이지면 null") Long nextCursor) {
    public static MemberPairingFeedbacksResponse from(MemberPairingFeedbacksResult result) {
        return new MemberPairingFeedbacksResponse(
                result.feedbacks().stream().map(MemberPairingFeedbackResponse::from).toList(),
                result.nextCursor());
    }
}
