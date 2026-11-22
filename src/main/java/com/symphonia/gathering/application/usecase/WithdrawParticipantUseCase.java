package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.WithdrawParticipantCommand;

public interface WithdrawParticipantUseCase {
    void withdraw(WithdrawParticipantCommand command);
}
