package com.symphonia.auth.application.usecase;

import com.symphonia.auth.application.dto.command.LoginCommand;
import com.symphonia.auth.application.dto.result.TokenResult;

public interface LoginUseCase {
    TokenResult login(LoginCommand command);
}
