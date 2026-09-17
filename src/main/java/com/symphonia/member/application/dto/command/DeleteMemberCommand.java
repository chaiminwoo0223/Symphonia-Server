package com.symphonia.member.application.dto.command;

public record DeleteMemberCommand(Long memberId, String accessToken, String ip) {
    public static DeleteMemberCommand of(Long memberId, String accessToken, String ip) {
        return new DeleteMemberCommand(memberId, accessToken, ip);
    }
}
