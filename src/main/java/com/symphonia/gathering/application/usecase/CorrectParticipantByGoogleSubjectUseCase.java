package com.symphonia.gathering.application.usecase;

import com.symphonia.gathering.application.dto.command.CorrectParticipantByGoogleSubjectCommand;

public interface CorrectParticipantByGoogleSubjectUseCase {
    void correctByGoogleSubject(CorrectParticipantByGoogleSubjectCommand command);
}
