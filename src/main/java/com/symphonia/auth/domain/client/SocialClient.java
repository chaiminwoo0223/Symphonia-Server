package com.symphonia.auth.domain.client;

import com.symphonia.auth.domain.identity.SocialIdentity;

public interface SocialClient {
    // 클라이언트 앱이 소셜 SDK로 받은 토큰을 검증하고 신원을 반환한다.
    // 구현체는 이 서비스를 위해 발급된 토큰일 때만 신원을 반환해야 한다.
    SocialIdentity authenticate(String token);
}
