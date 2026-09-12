package com.symphonia.auth.application.usecase;

import com.symphonia.auth.application.dto.command.RefreshCommand;
import com.symphonia.auth.application.dto.result.TokenResult;

public interface RefreshUseCase {
    TokenResult refresh(RefreshCommand command);
}
