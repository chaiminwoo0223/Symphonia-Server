package com.symphonia.auth.application.usecase;

import com.symphonia.auth.application.dto.result.TokenResult;

public interface RefreshUseCase {
    TokenResult refresh(String refreshToken);
}
