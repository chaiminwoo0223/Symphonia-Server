package com.symphonia.pairing.application.dto.result;

import java.util.List;

// nextCursor는 다음 페이지를 읽을 때 보내는 값이고, 마지막 페이지면 null이다.
public record MemberPairingFeedbacksResult(
        List<MemberPairingFeedbackResult> feedbacks, Long nextCursor) {
    public static MemberPairingFeedbacksResult of(
            List<MemberPairingFeedbackResult> feedbacks, Long nextCursor) {
        return new MemberPairingFeedbacksResult(feedbacks, nextCursor);
    }
}
