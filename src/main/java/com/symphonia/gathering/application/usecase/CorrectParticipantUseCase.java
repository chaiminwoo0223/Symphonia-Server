package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.CorrectParticipantCommand;

public interface CorrectParticipantUseCase {
    void correct(CorrectParticipantCommand command);
}
