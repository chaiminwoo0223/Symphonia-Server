package com.symphonia.auth.application.dto.command;

import com.symphonia.common.audit.HasIp;

// 로그아웃은 토큰이 없거나 만료돼도 처리하므로 refreshToken과 accessToken은 null일 수 있다.
public record LogoutCommand(String refreshToken, String accessToken, String ip) implements HasIp {
    public static LogoutCommand of(String refreshToken, String accessToken, String ip) {
        return new LogoutCommand(refreshToken, accessToken, ip);
    }
}
