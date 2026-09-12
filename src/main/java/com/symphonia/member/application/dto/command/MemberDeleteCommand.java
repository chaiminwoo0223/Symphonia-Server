package com.symphonia.member.application.dto.command;

public record MemberDeleteCommand(Long memberId, String accessToken, String ip) {
    public static MemberDeleteCommand of(Long memberId, String accessToken, String ip) {
        return new MemberDeleteCommand(memberId, accessToken, ip);
    }
}
