package com.symphonia.auth.application.usecase;

import com.symphonia.auth.application.dto.result.TokenResult;

public interface ReissueUseCase {
    TokenResult reissue(String refreshToken);
}
