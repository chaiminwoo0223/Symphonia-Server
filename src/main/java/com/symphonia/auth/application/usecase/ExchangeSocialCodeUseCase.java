package com.symphonia.auth.application.usecase;

import com.symphonia.auth.application.dto.result.OAuthMemberResult;

public interface ExchangeSocialCodeUseCase {
    OAuthMemberResult exchange(String provider, String code);
}
