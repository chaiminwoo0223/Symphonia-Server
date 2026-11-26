package com.symphonia.gathering.application.event;

public record ParticipantJoinedEvent(JoinType type) {
    public static ParticipantJoinedEvent byQr() {
        return new ParticipantJoinedEvent(JoinType.QR);
    }

    public static ParticipantJoinedEvent byHost() {
        return new ParticipantJoinedEvent(JoinType.HOST);
    }

    // 참여한 경로다. QR은 참여 토큰으로 참여한 경우이고, HOST는 모임장이 로그인한 채로 직접 참여한 경우다.
    public enum JoinType {
        QR,
        HOST
    }
}
