package com.symphonia.auth.helper;

import com.symphonia.auth.infrastructure.provider.AccessTokenProvider;
import com.symphonia.common.constants.HttpConstants;
import com.symphonia.member.domain.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthHelper {

    private final AccessTokenProvider accessTokenProvider;

    public String generateAccessToken(String memberId, String role) {
        return accessTokenProvider.generate(memberId, role);
    }

    public String bearerHeader(String accessToken) {
        return HttpConstants.BEARER_PREFIX + accessToken;
    }

    public String bearerTokenFor(Member member) {
        String accessToken =
                generateAccessToken(String.valueOf(member.getId()), member.getRole().name());

        return bearerHeader(accessToken);
    }
}
