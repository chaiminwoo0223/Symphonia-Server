package com.symphonia.auth.application.dto.result;

import com.symphonia.common.audit.HasActorId;

// 리프레시 토큰으로 회원을 찾지 못하면 memberId는 null이고, 감사 로그는 인증 정보의 회원으로 남는다.
public record LogoutResult(String memberId) implements HasActorId {
    public static LogoutResult of(String memberId) {
        return new LogoutResult(memberId);
    }

    @Override
    public String actorId() {
        return memberId;
    }
}
