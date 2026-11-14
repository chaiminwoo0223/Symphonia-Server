package com.symphonia.auth.application.usecase;

import com.symphonia.auth.application.dto.result.OAuthMemberResult;

public interface VerifySocialTokenUseCase {
    OAuthMemberResult verify(String provider, String token);
}
