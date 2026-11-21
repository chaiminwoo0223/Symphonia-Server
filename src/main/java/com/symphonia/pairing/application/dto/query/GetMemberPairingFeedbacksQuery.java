package com.symphonia.pairing.application.dto.query;

// cursor는 이전 페이지의 마지막 피드백 id이고, 첫 페이지면 null이다.
public record GetMemberPairingFeedbacksQuery(Long memberId, Long cursor, int size) {
    public static GetMemberPairingFeedbacksQuery of(Long memberId, Long cursor, int size) {
        return new GetMemberPairingFeedbacksQuery(memberId, cursor, size);
    }
}
