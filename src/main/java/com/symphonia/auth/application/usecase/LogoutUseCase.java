package com.symphonia.auth.application.usecase;

import com.symphonia.auth.application.dto.command.LogoutCommand;

public interface LogoutUseCase {
    void logout(LogoutCommand command);
}
