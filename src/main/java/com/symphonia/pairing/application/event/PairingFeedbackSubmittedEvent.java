package com.symphonia.pairing.application.event;

// 피드백이 새로 생긴 경우에만 발행한다. 회원이 같은 조합에 다시 낸 것은 평점 변경이므로 발행하지 않는다.
public record PairingFeedbackSubmittedEvent(SubmitterType type) {
    public static PairingFeedbackSubmittedEvent byMember() {
        return new PairingFeedbackSubmittedEvent(SubmitterType.MEMBER);
    }

    public static PairingFeedbackSubmittedEvent byParticipant() {
        return new PairingFeedbackSubmittedEvent(SubmitterType.PARTICIPANT);
    }

    // 제출한 경로다. PARTICIPANT는 모임 참여자가 익명으로 낸 경우다.
    public enum SubmitterType {
        MEMBER,
        PARTICIPANT
    }
}
