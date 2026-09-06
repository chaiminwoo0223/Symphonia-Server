package com.symphonia.auth.application.usecase;

import com.symphonia.auth.application.dto.result.TokenResult;

public interface IssueTokenUseCase {
    TokenResult issue(String memberId, String role);
}
