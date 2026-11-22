package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.RemoveParticipantBySubjectCommand;

public interface RemoveParticipantBySubjectUseCase {
    void remove(RemoveParticipantBySubjectCommand command);
}
