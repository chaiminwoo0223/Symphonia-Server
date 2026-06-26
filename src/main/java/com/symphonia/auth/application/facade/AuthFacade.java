package com.symphonia.auth.application.facade;

import com.symphonia.auth.application.dto.result.TokenResult;
import com.symphonia.auth.application.service.TokenService;
import com.symphonia.member.application.dto.result.MemberResult;
import com.symphonia.member.application.service.MemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthFacade {
    private final TokenService tokenService;
    private final MemberQueryService memberQueryService;

    public TokenResult reissue(String refreshToken) {
        String memberId = tokenService.getMemberId(refreshToken);
        MemberResult result = memberQueryService.getActiveById(Long.parseLong(memberId));

        return tokenService.reissue(memberId, result.role().name());
    }

    public void revoke(String accessToken) {
        tokenService.revoke(accessToken);
    }
}
