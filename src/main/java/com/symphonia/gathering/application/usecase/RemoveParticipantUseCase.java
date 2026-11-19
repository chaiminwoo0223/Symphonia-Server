package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.RemoveParticipantCommand;

public interface RemoveParticipantUseCase {
    void remove(RemoveParticipantCommand command);
}
