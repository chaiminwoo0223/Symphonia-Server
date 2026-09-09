package com.symphonia.auth.application.usecase;

import com.symphonia.auth.application.dto.command.SignupCommand;
import com.symphonia.auth.application.dto.result.TokenResult;

public interface SignupUseCase {
    TokenResult signup(SignupCommand command);
}
