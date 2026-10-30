package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.RegisterBankAccountCommand;

public interface RegisterBankAccountUseCase {
    void register(RegisterBankAccountCommand command);
}
