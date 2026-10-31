package com.symphonia.auth.application.usecase;

import com.symphonia.auth.application.dto.command.LogoutCommand;
import com.symphonia.auth.application.dto.result.LogoutResult;

public interface LogoutUseCase {
    LogoutResult logout(LogoutCommand command);
}
