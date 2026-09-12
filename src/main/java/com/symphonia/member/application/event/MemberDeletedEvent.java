package com.symphonia.member.application.event;

public record MemberDeletedEvent(Long memberId, String accessToken, String ip) {
    public static MemberDeletedEvent of(Long memberId, String accessToken, String ip) {
        return new MemberDeletedEvent(memberId, accessToken, ip);
    }
}
